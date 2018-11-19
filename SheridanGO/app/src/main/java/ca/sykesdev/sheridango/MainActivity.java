package ca.sykesdev.sheridango;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity{

    // This applications requestCode and other important constants
    public static final String DISPLAY_NAME_KEY = "displayName";
    public static final String APPLICATION_EXIT_TIME_KEY = "exitTime";
    public static final String USER_LIST_KEY_PARENT = "users";
    public static final String PROPERTY_DB_REF_KEY = "properties";
    public static final String USER_CASH_KEY = "totalCash";
    public static final String USER_PROPERTY_TOTAL_VALUE = "totalPropertyValue";
    public static final String USER_REVENUE_GAIN_KEY = "totalRevenueValue";
    public static final String USER_MY_PROPERTIES_KEY = "currentProperties";
    public static final String USER_PROP_OWNED_AMOUNT = "propOwnedAmount";
    public static final String USER_PROP_CASH_BENEFITS_AMOUNT = "cashBenefitsAmount";

    public static final int MAIN_ACTIVITY = 0; // IMPORTANT (DO NOT DELETE)
    private final String TAG = "MAIN_ACTIVITY";
    private final int ERROR_DIALOG_REQUEST = 9001;
    private final String ACCESS_FINE_LOCATION_PERM = Manifest.permission.ACCESS_FINE_LOCATION;
    private final String ACCESS_COARSE_LOCATION_PERM = Manifest.permission.ACCESS_COARSE_LOCATION;
    private boolean mLocationPermissionsGranted = false;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 9002;
    private SharedPreferences curUserPrefs;
    private TimerTask mIncomeTmrTask;
    private double mBackgroundPay;

    // firebase DB variables
    private static final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference mUsersData = mDatabase.getReference(USER_LIST_KEY_PARENT);
    public static DatabaseReference mCurUserDataRef;

    // Declare Control variables
    private ImageButton btnMyProperties;
    private TextView txtMyProperties, txtDisplayName, txtUserCash, txtPropertyValue,
            txtRevenueGained;
    private ProgressBar progAggregatePropertyValue;
    private Button btnShowAvailableProperties, btnOpenPremiumCashShop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create SharedPreferences Link
        curUserPrefs = PreferenceManager.
                getDefaultSharedPreferences(getApplicationContext());

        // ------- Init Control variables ------- //
        btnShowAvailableProperties = findViewById(R.id.btnShowAvailable);
        btnOpenPremiumCashShop = findViewById(R.id.btnPremiumShop);
        progAggregatePropertyValue = findViewById(R.id.progAggregatePropertyValue);

        // Data Fields
        txtDisplayName = findViewById(R.id.txtUserDisplayName);
        txtUserCash = findViewById(R.id.txtDisplayCash);
        txtPropertyValue = findViewById(R.id.txtAggregatePropertyValue);
        txtRevenueGained = findViewById(R.id.txtRevenueGained);


        // MyProperties Button Group
        btnMyProperties = findViewById(R.id.btnMyProperties);
        btnMyProperties.setOnClickListener(new MyPropertiesClickListener());
        txtMyProperties = findViewById(R.id.txtMyProperties);
        txtMyProperties.setOnClickListener(new MyPropertiesClickListener());

        // Setup firebase database autosync listener for data change
        mUsersData.addValueEventListener(rootDataReferenceListener);

        // Check that the user has an "account" with us & associates databases
        if (isServicesOK()) {
            checkLogon();
            getDatabase();
        } else {
            Log.e(TAG, "onCreate: Fatal error: Application will not work " +
                    "without correct Google Play Services");
        }

        if (curUserPrefs.getString(DISPLAY_NAME_KEY, "NULL").
                equalsIgnoreCase("NULL")) {
            Log.e(TAG, "onCreate: Cannot pay for offline time when this " +
                    "is the first time the user is opening the app!");

        } else {
            // Pay the user for time since they last opened the app
            payForOfflineTime();

            // Setup paying service for the user
            setupPayServices();
        }

        // Set Name for the current user HERE (Don't need to update constantly for obvious reasons)
        txtDisplayName.setText(String.format(getString(R.string.txt_username_displayname),
                curUserPrefs.getString(DISPLAY_NAME_KEY, "NULL")));

        // Setup Button Listeners
        /**
         * Open the available properties window
         */
        btnShowAvailableProperties.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent availablePropertiesIntent = new Intent(getApplicationContext(),
                        ShowPropertiesActivity.class);
                startActivityForResult(availablePropertiesIntent, MAIN_ACTIVITY);
            }
        });

        /**
         * DO NOTHING RIGHT NOW (NOT IMPLEMENTED!)
         * Tell user though!
         */
        btnOpenPremiumCashShop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Feature not implemented yet!" +
                        " Please try again later...", Toast.LENGTH_LONG).show();
                Log.e(TAG, "User tried to use a feature not implemented yet.");
            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
        // Set time when user exited the application
        SharedPreferences.Editor editor = curUserPrefs.edit();
        editor.putLong(APPLICATION_EXIT_TIME_KEY, System.currentTimeMillis());
        editor.apply();

        Log.i(TAG, "onDestroy: Saved time of app exit.");
    }

    /**
     * Save time of exit when destroying application
     */

    /**
     * Pay the user for the time since they have been offline
     */
    private  void payForOfflineTime() {
        long timeSinceExit = (System.currentTimeMillis() -
                curUserPrefs.getLong(APPLICATION_EXIT_TIME_KEY, 0));
        getIncomeForTime(timeSinceExit);

        Log.i(TAG, "payForOfflineTime: Paid user for time since they have been offline.");
    }

    /**
     * Gets income rel to the time of last payout...
     * @param timeMilisFromLastExecute Time in miliseconds since last execution
     */
    @SuppressLint("DefaultLocale")
    private void getIncomeForTime(final long timeMilisFromLastExecute) {
        Log.i(TAG, "GetIncomeForTime: Getting cash income from all owned properties...");
        final double TIME_HOURS = (timeMilisFromLastExecute / 1000.0 / 60.0 / 60.0);

        // Update income for current user..
        mCurUserDataRef.
                addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        boolean payingForBackground = (mBackgroundPay == 0);
                        mBackgroundPay = 0;
                        // Get total amount that needs to be paid out to the user this hour...
                        for (DataSnapshot ds : dataSnapshot.
                                child(USER_MY_PROPERTIES_KEY).getChildren()) {
                            mBackgroundPay += ((double) ds.
                                    child(USER_PROP_CASH_BENEFITS_AMOUNT).getValue()) * TIME_HOURS;
                        }

                        // Set users new cash amount to current plus the total paid for this hour
                        mCurUserDataRef.child(USER_CASH_KEY).
                                setValue(dataSnapshot.child(USER_CASH_KEY).
                                        getValue(Double.class) + mBackgroundPay);

                        // Also remember to update the users revenue
                        mCurUserDataRef.child(USER_REVENUE_GAIN_KEY).setValue(
                                dataSnapshot.child(USER_REVENUE_GAIN_KEY).getValue(Double.class)
                                        + mBackgroundPay
                        );

                        if (payingForBackground) {
                            // Display message to user
                            String dialogText = String.format(getString(R.string.
                                            dialoge_background_pay_notification_text),
                                    mBackgroundPay,
                                    TimeUnit.MILLISECONDS.toHours(timeMilisFromLastExecute),
                                    TimeUnit.MILLISECONDS.toMinutes(timeMilisFromLastExecute) -
                                            TimeUnit.MINUTES.toMinutes(TimeUnit.MILLISECONDS.toHours(timeMilisFromLastExecute)),
                                    TimeUnit.MILLISECONDS.toSeconds(timeMilisFromLastExecute) -
                                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeMilisFromLastExecute)));

                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setMessage(dialogText)
                                    .setCancelable(false)
                                    .setPositiveButton("AWESOME!", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            Log.i(TAG, "onDialogOkayClick: Returning to Activity");
                                        }
                                    });
                            AlertDialog alert = builder.create();
                            alert.show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Display error
                        Log.e(TAG, "onCancelled: Error: Security access or " +
                                "Remote server error..cannot access database.");
                    }
                });

    }

    /**
     * Checks to make sure googlePlayServices are running OK
     * @return true if all is running properly
     */
    private boolean isServicesOK() {
        Log.d(TAG, "isServicesOK: Checking if google play services is okay...");

        int available = GoogleApiAvailability.getInstance().
                isGooglePlayServicesAvailable(getApplicationContext());

        if (available == ConnectionResult.SUCCESS) {
            // Everything is fine, user can make requests...
            Log.d(TAG, "isServicesOK: Google play services is working perfectly!");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            // An error occurred with Google Play Services, but we should be able to fix it.
            Log.d(TAG, "isServicesOK: An error occurred, trying to fix it...");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this,
                    available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "We can't make map requests :(",
                    Toast.LENGTH_LONG);
            Log.e(TAG, "isServicesOK: Error: Cannot use Google Play Services... So no Map");
        }
        return false;
    }

    /**
     * Set the Database reference to the current users database reference.
     * This will save a lot of time later...
     */
    private void getDatabase() {
        mCurUserDataRef = mUsersData.child(curUserPrefs.getString(DISPLAY_NAME_KEY,
                "NULL"));
    }

    /**
     * Used to get open MyPropertiesActivity.class
     */
    private class MyPropertiesClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent myPropertiesIntent = new Intent(getApplicationContext(),
                    MyPropertiesActivity.class);

            startActivityForResult(myPropertiesIntent, MAIN_ACTIVITY);
        }
    }

    /**
     * Used whenever dashboard is returned to... Just some maintenance and user creation code here
     * @param requestCode The request code that opened the activity being returned from
     * @param resultCode How did it go?
     * @param data Pass any data if needed here...
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MAIN_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                Log.i(TAG, "onActivityResult: Everything is working properly. Nothing to " +
                        "report");
            } else if (resultCode == RESULT_FIRST_USER) {
                setupNewUser(data.getStringExtra(DISPLAY_NAME_KEY));
            } else {
                Log.i(TAG, "onActivityResult: No new information returned...");
                checkLogon();
            }
        }
    }

    /**
     * Update all values after returning from another Activity or if the values have changed since
     * last update. ( usually only called after db onDataChanged() )
     */
    private void updateDashValues(ArrayList<String> user_data) {
        // Fill in information to data fields...
        txtUserCash.setText(String.format(getString(R.string.txt_cash_text_value),
                Double.parseDouble(user_data.get(0))));
        txtPropertyValue.setText(String.format(getString(R.string.txt_property_value_text),
                Double.parseDouble(user_data.get(1))));
        txtRevenueGained.setText(String.format(getString(R.string.txt_revenue_value_text),
                Double.parseDouble(user_data.get(2))));

        // Set progress update (to value of property progress)
        progAggregatePropertyValue.setProgress(Integer.parseInt(user_data.get(1)));

        // Check for user hitting score/level cap and congratulate them if necessary!
        if (Integer.parseInt(user_data.get(1)) == progAggregatePropertyValue.getMax()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.text_user_hit_level_cap))
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Log.i(TAG, "onClick: Player reached level cap!" +
                                    " Must uninstall to create new account to play again");
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    /**
     * Checks login values to see if the user is logged in. If not then open login activity,
     * if yes, do nothing..
     */
    private void checkLogon() {
        // Check if this is the first time the user is opening the APP...
        if (curUserPrefs.getString(DISPLAY_NAME_KEY, "NULL").
                equalsIgnoreCase("NULL")) {
            Intent loginIntent = new Intent(getApplicationContext(),
                    LoginActivity.class);
            startActivityForResult(loginIntent, MAIN_ACTIVITY);
        }
    }

    /**
     * Performs all setup for a new user of the App.
     * @param displayName The users selected display name.
     */
    private void setupNewUser(String displayName) {
        // Get editor for saving users Display Name
        SharedPreferences.Editor editor = curUserPrefs.edit();

        // Store Display Name for user.
        editor.putString(DISPLAY_NAME_KEY, displayName);
        editor.apply(); // Apply changes

        // Tell user whats gwanin
        Toast.makeText(MainActivity.this,
                String.format("Welcome! Your display name has been set to %s",
                        curUserPrefs.getString(DISPLAY_NAME_KEY, "NULL")),
                Toast.LENGTH_LONG).show();

        // Setup dataBase stuff for this user...
        getDatabase();
        mCurUserDataRef.child(USER_CASH_KEY).setValue(50000.0);
        mCurUserDataRef.child(USER_PROPERTY_TOTAL_VALUE).setValue(0);
        mCurUserDataRef.child(USER_REVENUE_GAIN_KEY).setValue(0);
        mCurUserDataRef.child(USER_MY_PROPERTIES_KEY); // Just create the store location for now.

        // Set name on creation of user
        txtDisplayName.setText(String.format(getString(R.string.txt_username_displayname),
                curUserPrefs.getString(DISPLAY_NAME_KEY, "NULL")));
    }

    /**
     * Setup the services required to pay the user for properties owned
     */
    private void setupPayServices() {
        // Create incomeTimer for handling income from properties
        mIncomeTmrTask = new TimerTask() {
            @Override
            public void run() {
                getIncomeForTime(10000L);
            }
        };

        // Start the onGoing timer to get cash!
        Timer incomeTmr = new Timer();
        incomeTmr.schedule(mIncomeTmrTask, 10000L, 12000L);
    }

    /**
     * Listens for changes in user data such as cash, property values, etc. Then updates data fields
     * accordingly. Should be running asyncronously all the time, so there is no need to call
     * updateDashValues anywhere else..
     */
    private ValueEventListener rootDataReferenceListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            ArrayList<String> user_data = new ArrayList<>();
            String displayName = curUserPrefs.getString(DISPLAY_NAME_KEY, "NULL");


            // Add all user cash and score values to data arrayList
            try {
                if (dataSnapshot.exists() && dataSnapshot.hasChild(displayName)) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        if (ds.getKey().equalsIgnoreCase(displayName)) {
                            for (DataSnapshot valueObject : ds.getChildren()) {
                                // Make sure we are not loading in property names into dashboard as
                                // we don't need that information in this Activity.
                                if (!(valueObject.getKey().equalsIgnoreCase("currentProperties"))) {
                                    user_data.add(valueObject.getValue().toString());
                                }
                            }
                        }
                    }

                    // Update current dashboard values now...
                    updateDashValues(user_data);
                }
            } catch (IndexOutOfBoundsException e) {
                setupNewUser(displayName);
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            // Display error
            Log.e(TAG, "onCancelled: Error: Security access or " +
                    "Remote server error..cannot access database.");
        }
    };

    /**
     * Result from asking the user for permissions to use the APP
     * @param requestCode This Activities request code
     * @param permissions The permissions list
     * @param grantResults The permissions that have been granted..
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
}
