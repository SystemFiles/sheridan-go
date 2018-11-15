package ca.sykesdev.sheridango;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import model.PhotoHelper;
import model.Property;

public class NewPropertyManager extends AppCompatActivity {

    // Declare control variables
    private TextView txtNameOfProperty, txtPropertyInfo, txtCashCostInfo, txtCashBenefits;
    private EditText txtEnterInvestAmount;
    private ImageView imgPropertyPhoto;
    private Button btnBuy;

    // User DB Variables
    private DatabaseReference userReference = FirebaseDatabase.getInstance().
            getReference(MainActivity.USER_LIST_KEY_PARENT);

    // Activity Constants and Variables
    private final String TAG = "NEW_PROPERTY_MANAGER";
    private Property selectedProperty;
    private double cashBenefitsCalculated;
    private double userCashValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_property_manager);

        // Initialize costBenefits
        cashBenefitsCalculated = 0;

        // Get information from previous activity
        Intent intent = getIntent();
        selectedProperty =
                intent.getParcelableExtra(ShowPropertiesActivity.PROPERTY_LIST_INTENT_KEY);

        // Initialize Control Variables
        txtNameOfProperty = findViewById(R.id.txtNameOfProperty);
        txtPropertyInfo = findViewById(R.id.txtPropertyInfo);
        txtCashCostInfo = findViewById(R.id.txtCashCostValue);
        txtEnterInvestAmount = findViewById(R.id.txtBuyPercentage);
        txtCashBenefits = findViewById(R.id.txtCashBenefits);
        imgPropertyPhoto = findViewById(R.id.imgPropertyPhoto);
        btnBuy = findViewById(R.id.btnBuyShares);

        // Setup activity with all the information for selected activity
        initFields();
    }

    /**
     * Initializes the activity
     */
    @SuppressLint("SetTextI18n")
    private void initFields() {
        txtNameOfProperty.setText(selectedProperty.getmName());
        txtPropertyInfo.setText(selectedProperty.getmType().toUpperCase()
                + "  -  $" + selectedProperty.getmCost());
        txtCashCostInfo.setText(String.format(getString(R.string.txt_cost_text), 0.00));

        // Setup the photo of the place
        if (selectedProperty.getmPhotoID().equalsIgnoreCase("NULL")) {
            Log.i(TAG, "initFields: No photo associated with this property," +
                    " So we will just put a placeholder image");
            // Set the photo to placeholder
            imgPropertyPhoto.setImageDrawable(getDrawable(R.drawable.photo_not_found));
        } else {
            // Set the image to a requested image from Google
            GetPhotoTask photoTask = new GetPhotoTask();
            photoTask.execute();
        }

        // Preview invest cost and stuff...
        txtEnterInvestAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // DO nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Check first to make sure we are working with something.
                if (!(txtEnterInvestAmount.getText().toString().isEmpty())) {
                    int invAmountEntered = Integer.parseInt(txtEnterInvestAmount.getText().toString());

                    // Update the cash cost based on the percent the user would like to buy
                    if (invAmountEntered > 100 || invAmountEntered < 1) {
                        txtEnterInvestAmount.setError("Error: Enter percent value between 1-100.");
                    } else {
                        double costAmount = ((selectedProperty.getmCost() * ((double) invAmountEntered / 100))
                                * 1.100621224124 + 100);

                        // Update cost amount for user to see
                        costAmount = Math.round(costAmount * 100.0) / 100.0;
                        txtCashCostInfo.setText(String.format(getString(R.string.txt_cost_text), costAmount));

                        // Calculate cash benefits and display
                        cashBenefitsCalculated = ((costAmount / 150) + (costAmount * 0.22)) / 12;

                        // Update benefits for user
                        txtCashBenefits.setText(String.
                                format(getString(R.string.txt_cash_benefits_text),
                                        cashBenefitsCalculated));
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // DO NOTHING
            }
        });

        // Finally handle the investing here..
        btnBuy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                investInProperty();
            }
        });
    }


    /**
     * Handles investing in the selected property
     */
    private void investInProperty() {
        SharedPreferences userPrefs = PreferenceManager.
                getDefaultSharedPreferences(getApplicationContext());
        final String displayName = userPrefs.getString(MainActivity.DISPLAY_NAME_KEY, "NULL");
        userReference.child(displayName).
                addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                double costAmount = Double.parseDouble(txtCashCostInfo.
                        getText().subSequence(7,txtCashCostInfo.getText().length()).toString());
                double investAmountPercent = (Double.parseDouble(txtEnterInvestAmount.
                        getText().toString())) / 100;

                try {
                    double userPropertyValue = dataSnapshot.child(MainActivity.
                            USER_PROPERTY_TOTAL_VALUE).getValue(Double.class);
                    userCashValue = dataSnapshot.child(MainActivity.USER_CASH_KEY).getValue(Double.class);
                    boolean ownsAlready =
                            dataSnapshot.child(MainActivity.
                                    USER_MY_PROPERTIES_KEY).hasChild(selectedProperty.getmName());

                    // Check if user has enough money to make purchase
                    if ((costAmount <= userCashValue)) {
                        if (investAmountPercent <= (1 - selectedProperty.getmInvestAmount())) {
                            userReference.child(displayName).child(MainActivity.USER_CASH_KEY)
                                    .setValue((userCashValue - costAmount));

                            // Update investAmount on property
                            userReference.getParent().child(MainActivity.PROPERTY_DB_REF_KEY).
                                    child(selectedProperty.getmID()).
                                    child(ShowPropertiesActivity.PROPERTY_INVEST_TOTAL_KEY).
                                    setValue(selectedProperty.getmInvestAmount() +
                                            investAmountPercent);

                            // Update user property value holding
                            userReference.child(displayName).child(MainActivity.USER_PROPERTY_TOTAL_VALUE)
                                    .setValue(userPropertyValue + (selectedProperty.getmCost()
                                            * investAmountPercent));

                            // Check if the user already owns shares in the selected property
                            if (!ownsAlready) {
                                // Update user properties to include key of new property invested in
                                userReference.getParent().child(MainActivity.USER_LIST_KEY_PARENT).
                                        child(displayName).child(MainActivity.USER_MY_PROPERTIES_KEY)
                                        .child(selectedProperty.getmName()).setValue(selectedProperty.getmID());

                                // Add The amount owned to the property item.
                                userReference.getParent().child(MainActivity.USER_LIST_KEY_PARENT).
                                        child(displayName).child(MainActivity.USER_MY_PROPERTIES_KEY)
                                        .child(selectedProperty.getmName()).
                                        child(MainActivity.USER_PROP_OWNED_AMOUNT).
                                        setValue(investAmountPercent);

                            } else {
                                // If the user does own shares, get the amount owned and update
                                double currentOwnedAmount = (double) dataSnapshot.
                                        child(MainActivity.USER_MY_PROPERTIES_KEY).
                                        child(selectedProperty.getmName()).
                                        child(MainActivity.USER_PROP_OWNED_AMOUNT).getValue();

                                userReference.getParent().child(MainActivity.USER_LIST_KEY_PARENT).
                                        child(displayName).child(MainActivity.USER_MY_PROPERTIES_KEY)
                                        .child(selectedProperty.getmName()).
                                        child(MainActivity.USER_PROP_OWNED_AMOUNT).
                                        setValue(currentOwnedAmount + investAmountPercent);
                            }

                            // Setting cashBenefitsCalculated to database so we can get it later
                            // from the database...
                            userReference.child(displayName)
                                    .child(MainActivity.USER_MY_PROPERTIES_KEY).
                                    child(selectedProperty.getmName())
                                    .child(MainActivity.USER_PROP_CASH_BENEFITS_AMOUNT).
                                    setValue(cashBenefitsCalculated);

                        } else {
                            Toast.makeText(getApplicationContext(),
                                    "Sorry, it seems you want to invest too much in this property!",
                                    Toast.LENGTH_LONG).show();
                            Log.e(TAG, "investInProperty: Property cannot hold that much more investment.");
                        }
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "It seems you do not have the funds required to make this purchase",
                                Toast.LENGTH_LONG).show();
                        Log.e(TAG, "investInProperty: User did not have enough" +
                                " money to invest in property!");
                    }
                } catch (NullPointerException e) {
                    Log.e(TAG, "onDataChange: Problem getting cash for user...");
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
     * Handles the photo retrieval...
     */
    private class GetPhotoTask extends AsyncTask<Void, Void, Bitmap> {

        @Override
        protected void onPreExecute() {
            Log.i(TAG, "onPreExecute: Getting photo for property...");
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            PhotoHelper helper = new PhotoHelper(selectedProperty.getmPhotoID());
            return helper.getPhotoFromID();
        }

        @Override
        protected void onPostExecute(Bitmap propImg) {
            Log.i(TAG, "onPostExecute: Got image for property! Setting it now..");
            imgPropertyPhoto.setImageBitmap(propImg);
        }
    }


}
