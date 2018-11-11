package ca.sykesdev.sheridango;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import model.PlacesHelper;
import model.PlacesRecyclerAdapter;
import model.Property;

public class ShowPropertiesActivity extends AppCompatActivity {

    // Control Variables
    private Button btnRefresh;
    private RecyclerView rAvailablePropertyView;

    // Location Variables and Places
    private LocationManager locationManager;
    private double mLatitude;
    private double mLongitude;
    private ArrayList<Property> propertiesList;

    // Constants for Properties DB Interaction/Parsing
    private DatabaseReference mPropertyDataRef = FirebaseDatabase.getInstance()
            .getReference(MainActivity.PROPERTY_DB_REF_KEY);
    public static final String PROPERTY_NAME_KEY = "propertyName";
    public static final String PROPERTY_TYPE_KEY = "propertyType";
    public static final String PROPERTY_PHOTO_ID_KEY = "propertyPhotoID";
    public static final String PROPERTY_VALUE_KEY = "propertyValue";
    public static final String PROPERTY_INVEST_TOTAL_KEY = "propertyInvested";

    // Activity constants && variables
    private SharedPreferences curUserPrefs;
    public static final int SHOW_PROPERTIES_ACTIVITY = 1; // IMPORTANT (DO NOT DELETE)
    private final String TAG = "SHOW_PROPERTIES_ACT";
    private final int ERROR_DIALOG_REQUEST = 9001;
    private final String ACCESS_FINE_LOCATION_PERM = Manifest.permission.ACCESS_FINE_LOCATION;
    private final String ACCESS_COARSE_LOCATION_PERM = Manifest.permission.ACCESS_COARSE_LOCATION;
    private boolean mLocationPermissionsGranted = false;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 9002;

    // Suppressing warning because we are checking for location before anyways (no need to check twice)
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_properties);

        // Get location permissions and start tracking location
        getLocationPermissions();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                5000, 10, locationListener);

        // Get User Preferences for use later
        curUserPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Init Control Variables
        btnRefresh = findViewById(R.id.btnRefresh);
        rAvailablePropertyView = findViewById(R.id.rAvailableProperties);
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                RequestPlacesTask requestPlacesTask = new RequestPlacesTask();
                requestPlacesTask.execute(location.getLatitude(),
                        location.getLongitude(), 1500.0);
            }
        });
    }

    /**
     * Method called by event handler to load the list of data to display in RecyclerView...
     * AKA: PlacesRecyclerView
     */
    private void listPlaces() {

        Log.i(TAG, "loadPlaces: Listing properties into recyclerView...");

        // For performance optimization we set each item to a fixed size..
        rAvailablePropertyView.setHasFixedSize(true);

        // Set up a spacer for the list.. (line between rows)
        rAvailablePropertyView.addItemDecoration(new DividerItemDecoration(
                rAvailablePropertyView.getContext(), DividerItemDecoration.VERTICAL));

        // Create LinearLayour Manager and set it to vertical layout orientation
        RecyclerView.LayoutManager manager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);

        // Assign the layout manager to our recyclerview
        rAvailablePropertyView.setLayoutManager(manager);

        // Create the adapter and assign it to the RecyclerView
        PlacesRecyclerAdapter adapter = new PlacesRecyclerAdapter(propertiesList);
        rAvailablePropertyView.setAdapter(adapter);
    }

    /**
     * Get required location permissions for app.
     */
    private void getLocationPermissions() {
        String[] permissions = {android.Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                ACCESS_FINE_LOCATION_PERM) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    ACCESS_COARSE_LOCATION_PERM) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    /**
     * What to do after we request needed permissions
     * @param requestCode The Request Code for this type of request
     * @param permissions List of required permissions
     * @param grantResults The results of the operation (did we get the permissions?)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionsGranted = false;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false;
                            Log.e(TAG, "onRequestPermissionsResult: Error: Could not get the required permissions..App will not work!");
                            break;
                        }
                    }

                    mLocationPermissionsGranted = true;
                }
            }
        }
    }

    /**
     * Listens for changes in property data
     * Should be running asyncronously all the time, so there is no need to call.
     */
    private ValueEventListener propertyDataListener = new ValueEventListener() {

        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            int position = 0;
            ArrayList<String> updatedList = new ArrayList<>();

            for (Property p : propertiesList) {
                if (dataSnapshot.exists() && dataSnapshot.hasChild(p.getmID())) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        if (ds.getKey().equalsIgnoreCase(p.getmID())) {
                            try {
                                // If found (or exists)
                                propertiesList.get(position).setmCost(
                                        ds.child(PROPERTY_VALUE_KEY).getValue(double.class));
                                propertiesList.get(position).setmInvestAmount(
                                        ds.child(PROPERTY_INVEST_TOTAL_KEY).getValue(double.class));
                                // Mark property as updated to keep track...
                                updatedList.add(ds.getKey());
                                break;
                            } catch (NullPointerException e) {
                                Log.e(TAG, "onDataChange: Null Object" +
                                        " reference when assigning values to children in DB" +
                                        " NOTE: This is not a fatal error don't worry!");
                            }
                        }
                    }
                    // Iterate through places..
                    position++;
                }
            }

            // Check for all places that did not exist or get updated and add them individually..
            if (updatedList.size() < propertiesList.size()) {
                for (int i = 0; i < propertiesList.size(); i++) {
                    if (!(updatedList.contains(propertiesList.get(i).getmID()))) {
                        initPlaceAt(i); // Initialize new property when found
                    }
                }
            }

            // Re-list places with new updated information.
            listPlaces();
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            // Display error
            Log.e(TAG, "onCancelled: Error: Security access or " +
                    "Remote server error..cannot access database. EMessage: " + databaseError.getMessage());
        }
    };

    /**
     * Initializes the place at n
     */
    private void initPlaceAt(int n) {
        Property currentProperty = propertiesList.get(n);

        mPropertyDataRef.child(currentProperty.getmID()).
                child(PROPERTY_NAME_KEY).setValue(currentProperty.getmName());
        mPropertyDataRef.child(currentProperty.getmID()).
                child(PROPERTY_PHOTO_ID_KEY).setValue(currentProperty
                .getmPhotoID());
        mPropertyDataRef.child(currentProperty.getmID()).
                child(PROPERTY_TYPE_KEY).setValue(currentProperty
                .getmType());
        mPropertyDataRef.child(currentProperty.getmID()).
                child(PROPERTY_VALUE_KEY).setValue(currentProperty
                .getmCost());
        mPropertyDataRef.child(currentProperty.getmID()).
                child(PROPERTY_INVEST_TOTAL_KEY).setValue(currentProperty
                .getmInvestAmount());
    }

    /**
     * Initializes nearby properties in the database
     */
    private void initNearbyProperties() {
        for (int i = 0; i < propertiesList.size(); i ++) {
            initPlaceAt(i);
        }
    }

    /**
     * Task used to request place information for nearby places..
     */
    private class RequestPlacesTask extends AsyncTask<Double, Void, ArrayList<Property>> {

        @Override
        protected void onPreExecute() {
            Log.i(TAG, "onPreExecute: Retreiving Places Information");
        }

        @Override
        protected ArrayList<Property> doInBackground(Double... values) {
            PlacesHelper helper = new PlacesHelper(ShowPropertiesActivity.this);

            return helper.getProperties(values[0], values[1], values[2]);
        }

        @Override
        protected void onPostExecute(ArrayList<Property> propertiesList) {
            ShowPropertiesActivity.this.propertiesList = propertiesList; // Not sure if this is still necessary

            // Check database for existing data for nearby properties (if NONE, then initialize them)
            mPropertyDataRef.addValueEventListener(propertyDataListener);

            // Start Listing properties afterwards

        }
    }

    /**
     * Constantly update the users current location
     */
    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            // Update users location vector.
            mLatitude = location.getLatitude();
            mLongitude = location.getLongitude();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // Do nothing
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.i(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.i(TAG, "onProviderDisabled: " + provider);
        }
    };
}
