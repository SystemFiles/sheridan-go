package ca.sykesdev.sheridango;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import model.ClickListener;
import model.MyPropertysRecyclerAdapter;
import model.Property;

public class MyPropertiesActivity extends AppCompatActivity {

    // Control Variables
    private RecyclerView rMyPropertyView;

    // Firebase DB Reference for user properties
    DatabaseReference mRootDataReference;

    // Activity Constants and variables
    private ArrayList<Property> myPropertiesList = new ArrayList<>();
    public static final int MY_PROPERTIES_ACTIVITY = 2; // IMPORTANT (DO NOT DELETE)
    public static final String SELECTED_PROPERTY_INTENT_KEY = "selectedPropertyKey";
    private final String TAG = "MY_PROPERTIES_ACT";
    private SharedPreferences userPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_properties);

        // Get shared preferences
        userPrefs = PreferenceManager.
                getDefaultSharedPreferences(this);

        // Get DB reference needed to list owned properties
        mRootDataReference = FirebaseDatabase.
                getInstance().getReference();

        // Init RecyclerViewControl (Here so we can refresh)
        rMyPropertyView = findViewById(R.id.rMyPropertyView);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Re-Load the updated list of owned properties.
        loadProperties();
    }

    /**
     * Load in properties owned by the user..
     */
    private void loadProperties() {

        // Clear items so we can see a new updated (refreshed) views. (Only important on secondary loads)
        myPropertiesList.clear();

        // Update the local properties list to include updated data from DB
        mRootDataReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                DataSnapshot myPropReference = dataSnapshot.
                        child(MainActivity.USER_LIST_KEY_PARENT).
                        child(userPrefs.getString(MainActivity.DISPLAY_NAME_KEY, "NULL")).
                        child(MainActivity.USER_MY_PROPERTIES_KEY);
                DataSnapshot globalPropReference = dataSnapshot.
                        child(MainActivity.PROPERTY_DB_REF_KEY);

                // For each property we need (ID, name, owned, cost, incomeBenefits)
                for (DataSnapshot myProp : myPropReference.getChildren()) {
                    // Get special values from global
                    String myCurrentPropName = myProp.getKey();
                    String id = null;
                    double cost = 0.0;
                    String photoID = "NULL";

                    // Search for needed values (INSIDE the Global Props)
                    for (DataSnapshot globalProp : globalPropReference.getChildren()) {
                        if (globalProp.child(ShowPropertiesActivity.PROPERTY_NAME_KEY).
                                getValue().toString().
                                equalsIgnoreCase(myCurrentPropName)) {
                            id = globalProp.getKey();
                            cost = globalProp.child(ShowPropertiesActivity.
                                    PROPERTY_VALUE_KEY).getValue(Double.class);
                            photoID = globalProp.child(ShowPropertiesActivity.
                                    PROPERTY_PHOTO_ID_KEY).getValue(String.class);
                        }
                    }

                    /* Make sure the property actually exists in the global context of
                     this applications database
                    */
                    if (id != null) {
                        myPropertiesList.add(new Property(id,
                                myCurrentPropName,
                                photoID,
                                cost,
                                myProp.child(MainActivity.USER_PROP_OWNED_AMOUNT).
                                        getValue(Double.class),
                                myProp.child(MainActivity.USER_PROP_CASH_BENEFITS_AMOUNT).
                                        getValue(Double.class)));
                    }
                }

                // List the places in the recycler view
                listMyProperties();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Display error
                Log.e(TAG, "onCancelled: Error: Security access or " +
                        "Remote server error..cannot access database. EMessage: " + databaseError.getMessage());
            }
        });
    }

    /**
     * Method called by event handler to load the list of data to display in RecyclerView...
     * AKA: MyPropertyRecyclerView
     */
    private void listMyProperties() {
        Log.i(TAG, "listMyProperties: Listing properties into recyclerView...");

        // For performance optimization we set each item to a fixed size..
        rMyPropertyView.setHasFixedSize(true);

        // Set up a spacer for the list.. (line between rows)
        rMyPropertyView.addItemDecoration(new DividerItemDecoration(
                rMyPropertyView.getContext(), DividerItemDecoration.VERTICAL));

        // Create LinearLayour Manager and set it to vertical layout orientation
        RecyclerView.LayoutManager manager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);

        // Assign the layout manager to our recyclerview
        rMyPropertyView.setLayoutManager(manager);

        // Create the adapter and assign it to the RecyclerView
        MyPropertysRecyclerAdapter adapter = new MyPropertysRecyclerAdapter(myPropertiesList, new ClickListener() {
            @Override
            public void onPositionClicked(int position) {
                Intent newPropertyIntent = new Intent(getApplicationContext(),
                        MyPropertyManager.class);

                // pass property information to intent
                newPropertyIntent.putExtra(SELECTED_PROPERTY_INTENT_KEY,
                        myPropertiesList.get(position));
                startActivityForResult(newPropertyIntent, MY_PROPERTIES_ACTIVITY);
            }
        });
        rMyPropertyView.setAdapter(adapter);
    }


    /**
     * Refresh properties if result is okay.
     * @param requestCode The activity that opened the Activity being returned from
     * @param resultCode The result. How'd it go?
     * @param data NULL
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MY_PROPERTIES_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                // Refresh properties
                loadProperties();
            }
        }
    }
}
