package ca.sykesdev.sheridango;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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
import model.PlacesRecyclerAdapter;
import model.Property;

public class MyPropertiesActivity extends AppCompatActivity {

    // Control Variables
    private RecyclerView rMyPropertyView;

    // Firebase DB Reference for user properties
    DatabaseReference mOwnedPropertiesReference;

    // Activity Constants and variables
    private ArrayList<Property> myPropertiesList = new ArrayList<>();
    public static final int MY_PROPERTIES_ACTIVITY = 2; // IMPORTANT (DO NOT DELETE)
    public static final String MY_PROPERTY_LIST_INTENT_KEY = "myPropListIntent";
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
        mOwnedPropertiesReference = FirebaseDatabase.
                getInstance().getReference(MainActivity.
                USER_LIST_KEY_PARENT).child(userPrefs.
                getString(MainActivity.DISPLAY_NAME_KEY, "NULL")).
                child(MainActivity.USER_MY_PROPERTIES_KEY);

        // Load owned properties into arraylist and init controls
        rMyPropertyView = findViewById(R.id.rMyPropertyView);

        mOwnedPropertiesReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    myPropertiesList.add(new Property(ds.getKey(),
                            (double) ds.child(MainActivity.USER_PROP_CASH_BENEFITS_AMOUNT).
                                    getValue(),
                            (double) ds.child(MainActivity.USER_PROP_OWNED_AMOUNT).
                                    getValue()));
                }

                // List the places in the recycler view
                listPlaces();
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
    private void listPlaces() {

        Log.i(TAG, "listPlaces: Listing properties into recyclerView...");

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
                newPropertyIntent.putExtra(MY_PROPERTY_LIST_INTENT_KEY,
                        myPropertiesList.get(position));
                startActivityForResult(newPropertyIntent, MY_PROPERTIES_ACTIVITY);
            }
        });
        rMyPropertyView.setAdapter(adapter);
    }
}
