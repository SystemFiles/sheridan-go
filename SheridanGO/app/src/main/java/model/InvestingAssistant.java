package model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import ca.sykesdev.sheridango.MainActivity;
import ca.sykesdev.sheridango.ShowPropertiesActivity;

public class InvestingAssistant {

    // Class variables
    private String displayName;
    private Property selectedProperty;
    private DatabaseReference userReference;
    private double userCashValue;
    private double cashBenefitsCalculated;
    private double costAmount;
    private double investAmountPercent;
    private Context context;

    // Constants
    private final String TAG = "INVESTING_ASSISTANT";

    /**
     * Creates the investing assistant to help with handling investments in properties
     * @param selectedProperty The property to invest in
     * @param cashBenefitsCalculated The benefits received from investing in the property
     * @param costAmount The cost of the property
     * @param investAmountPercent The amount being invested
     * @param context The context for the object.
     */
    public InvestingAssistant(Property selectedProperty, double cashBenefitsCalculated, double costAmount, double investAmountPercent, Context context) {
        this.selectedProperty = selectedProperty;
        this.cashBenefitsCalculated = cashBenefitsCalculated;
        this.costAmount = costAmount;
        this.investAmountPercent = investAmountPercent;
        this.context = context;

        userReference = FirebaseDatabase.getInstance().getReference(MainActivity.USER_LIST_KEY_PARENT);

        // Get shared preferences for user
        SharedPreferences userPrefs = PreferenceManager.
                getDefaultSharedPreferences(context.getApplicationContext());
        displayName = userPrefs.getString(MainActivity.DISPLAY_NAME_KEY, "NULL");
    }

    /**
     * Handle selling of shares in a property
     */
    public void sellSharesFromProperty() {
        // TODO: Implement selling of shares...
        userReference.child(displayName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Check that the user actually owns the selectedProperty

                //
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
     * Handles investing in the selected property
     */
    public void investInProperty() {
        userReference.child(displayName).
                addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        try {
                            double userPropertyValue = dataSnapshot.child(MainActivity.
                                    USER_PROPERTY_TOTAL_VALUE).getValue(Double.class);
                            userCashValue = dataSnapshot.child(MainActivity.USER_CASH_KEY).getValue(Double.class);
                            boolean ownsAlready =
                                    dataSnapshot.child(MainActivity.
                                            USER_MY_PROPERTIES_KEY).hasChild(selectedProperty.getmName());
                            double oldBenefits = 0.0;

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

                                        oldBenefits = (double) dataSnapshot.
                                                child(MainActivity.USER_MY_PROPERTIES_KEY).
                                                child(selectedProperty.getmName()).
                                                child(MainActivity.USER_PROP_CASH_BENEFITS_AMOUNT).getValue();
                                    }

                                    // Setting cashBenefitsCalculated to database so we can get it later
                                    // from the database...
                                    userReference.child(displayName)
                                            .child(MainActivity.USER_MY_PROPERTIES_KEY).
                                            child(selectedProperty.getmName())
                                            .child(MainActivity.USER_PROP_CASH_BENEFITS_AMOUNT).
                                            setValue(oldBenefits + cashBenefitsCalculated);

                                } else {
                                    Toast.makeText(context.getApplicationContext(),
                                            "Sorry, it seems you want to invest too much in this property!",
                                            Toast.LENGTH_LONG).show();
                                    Log.e(TAG, "investInProperty: Property cannot hold that much more investment.");
                                }
                            } else {
                                Toast.makeText(context.getApplicationContext(),
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
     * Calculates the cash benefits for the given property being purchased with
     * respect to the cost of the property
     * @param costAmount The cost of the property
     * @return CashBenefits amount...
     */
    public static double calculateCashBenefits(double costAmount) {
        // Calculate cash benefits and display
        return ((costAmount / 150) + (costAmount * 0.22)) / 12;
    }


}
