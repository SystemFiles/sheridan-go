package ca.sykesdev.sheridango;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import ca.sykesdev.sheridango.interfaces.ClickListener;
import ca.sykesdev.sheridango.model.PlacesHelper;
import ca.sykesdev.sheridango.adapter.PlacesRecyclerAdapter;
import ca.sykesdev.sheridango.model.Property;

public class ShowPropertiesActivity extends AppCompatActivity {

    // Control Variables
    private Button btnRefresh;
    private RecyclerView rAvailablePropertyView;

    // Location Variables and Places
    private double mLatitude, mLongitude;
    private LocationManager locationManager;
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
    public static final String PROPERTY_LIST_INTENT_KEY = "propertyArrayList";
    private final String ACCESS_FINE_LOCATION_PERM = Manifest.permission.ACCESS_FINE_LOCATION;
    private final String ACCESS_COARSE_LOCATION_PERM = Manifest.permission.ACCESS_COARSE_LOCATION;
    private boolean mLocationPermissionsGranted = true;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 9002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_properties);

        // Setup everything for the activity
        setupActivity();
    }

    /**
     * Sets up the activity
     */
    private void setupActivity() {
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION_PERM)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION_PERM)
                != PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionsGranted = false;
            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{ACCESS_COARSE_LOCATION_PERM, ACCESS_FINE_LOCATION_PERM},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            if (mLocationPermissionsGranted) {
                Log.i(TAG, "onCreate: Location Permissions granted! Continuing");
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
                        @SuppressLint("MissingPermission") Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        RequestPlacesTask requestPlacesTask = new RequestPlacesTask();
                        requestPlacesTask.execute(location.getLatitude(),
                                location.getLongitude(), 500.0);
                    }
                });
            }
        }
    }

    /**
     * Method called by event handler to load the list of data to display in RecyclerView...
     * AKA: PlacesRecyclerView
     */
    private void listPlaces() {

        Log.i(TAG, "listPlaces: Listing properties into recyclerView...");

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
        PlacesRecyclerAdapter adapter = new PlacesRecyclerAdapter(propertiesList, new ClickListener() {
            @Override
            public void onPositionClicked(int position) {
                Intent newPropertyIntent = new Intent(getApplicationContext(),
                        NewPropertyManager.class);

                // pass property information to intent
                newPropertyIntent.putExtra(PROPERTY_LIST_INTENT_KEY,
                        propertiesList.get(position));
                startActivityForResult(newPropertyIntent, SHOW_PROPERTIES_ACTIVITY);
            }
        });
        rAvailablePropertyView.setAdapter(adapter);
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
     * Task used to request place information for nearby places..
     */
    private class RequestPlacesTask extends AsyncTask<Double, Integer, ArrayList<Property>> {

        @Override
        protected void onPreExecute() {
            Log.i(TAG, "onPreExecute: Retrieving Places Information..");
            btnRefresh.setText(getString(R.string.btn_refresh_loading_text));
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

            Log.i(TAG, "onPreExecute: Done getting places information!");

            // Reset refresh button
            btnRefresh.setText(getString(R.string.btn_refresh_text));
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == SHOW_PROPERTIES_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                Log.i(TAG, "onActivityResult: Returned from new property manager...");
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
                    setupActivity();
                }
            }
        }
    }
}
