package ca.sykesdev.sheridango.model;

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
import ca.sykesdev.sheridango.interfaces.OnWantToExitListener;

public class InvestingAssistant {

    // Class variables
    private String displayName;
    private Property selectedProperty;
    private DatabaseReference rootReference;
    private DatabaseReference userReference;
    private DatabaseReference propDataReference;
    private double userCashValue;
    private double cashBenefitsCalculated;
    private double costAmount;
    private double investSellAmountPercent;
    private Context context;
    private OnWantToExitListener exitListener;

    // Variables used in selling (SPLT)
    private double userPropOwnedAmount, globalPropertyOwned,
            userPropCashBenefits, userTotalPropertyValue;

    // Constant for logging
    private final String TAG = "INVESTING_ASSISTANT";

    /**
     * Creates the investing assistant to help with handling investments in properties
     *
     * @param selectedProperty        The property to invest in
     * @param cashBenefitsCalculated  The benefits received from investing in the property
     * @param costAmount              The cost of the property
     * @param investSellAmountPercent The amount being invested
     * @param context                 The context for the object.
     */
    public InvestingAssistant(Property selectedProperty, double cashBenefitsCalculated, double costAmount, double investSellAmountPercent, Context context) {
        this.selectedProperty = selectedProperty;
        this.cashBenefitsCalculated = cashBenefitsCalculated;
        this.costAmount = costAmount;
        this.investSellAmountPercent = investSellAmountPercent;
        this.context = context;

        rootReference = FirebaseDatabase.getInstance().getReference();
        userReference = FirebaseDatabase.getInstance().getReference(MainActivity.USER_LIST_KEY_PARENT);
        propDataReference = FirebaseDatabase.getInstance().getReference(MainActivity.PROPERTY_DB_REF_KEY);

        // Get shared preferences for user
        SharedPreferences userPrefs = PreferenceManager.
                getDefaultSharedPreferences(context.getApplicationContext());
        displayName = userPrefs.getString(MainActivity.DISPLAY_NAME_KEY, "NULL");
    }

    /**
     * Handle selling of shares in a property
     */
    public void sellSharesFromProperty() {
        rootReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Check that the user actually owns the selectedProperty
                if (dataSnapshot.child(MainActivity.USER_LIST_KEY_PARENT).child(displayName).child(MainActivity.
                        USER_MY_PROPERTIES_KEY).hasChild(selectedProperty.getmName())) {

                    // Convert investSellAmountPercent to a percentage(decimal)
                    investSellAmountPercent /= 100;

                    // ---------GET RE_USED VARIABLE AMOUNTS FROM DATABASE AND STORE ------------ //
                    userPropOwnedAmount = dataSnapshot.child(MainActivity.
                            USER_LIST_KEY_PARENT).
                            child(displayName).
                            child(MainActivity.USER_MY_PROPERTIES_KEY).
                            child(selectedProperty.getmName()).
                            child(MainActivity.USER_PROP_OWNED_AMOUNT).getValue(Double.class);

                    // Round number to avoid errors
                    userPropOwnedAmount = roundNumber(userPropOwnedAmount);

                    globalPropertyOwned = dataSnapshot.child(MainActivity.PROPERTY_DB_REF_KEY).
                            child(selectedProperty.getmID()).
                            child(ShowPropertiesActivity.PROPERTY_INVEST_TOTAL_KEY)
                            .getValue(Double.class);

                    userPropCashBenefits = dataSnapshot.child(MainActivity.USER_LIST_KEY_PARENT).
                            child(displayName).
                            child(MainActivity.USER_MY_PROPERTIES_KEY).
                            child(selectedProperty.getmName()).
                            child(MainActivity.USER_PROP_CASH_BENEFITS_AMOUNT).
                            getValue(Double.class);

                    userCashValue = dataSnapshot.child(MainActivity.USER_LIST_KEY_PARENT).
                            child(displayName).child(MainActivity.USER_CASH_KEY)
                            .getValue(Double.class);

                    userTotalPropertyValue = dataSnapshot.child(MainActivity.USER_LIST_KEY_PARENT)
                            .child(displayName).
                                    child(MainActivity.USER_PROPERTY_TOTAL_VALUE).
                                    getValue(Double.class);

                    // ------ END OF STORED VARIABLES FROM DB -------- //

                    // Check that the user wants to sell less than total they own of the property
                    if (investSellAmountPercent < userPropOwnedAmount && investSellAmountPercent > 0) {
                        // Get rid of property shares Global and Personal
                        userReference.child(displayName).child(MainActivity.USER_MY_PROPERTIES_KEY)
                                .child(selectedProperty.getmName()).
                                child(MainActivity.USER_PROP_OWNED_AMOUNT).setValue(
                                userPropOwnedAmount - investSellAmountPercent);

                        // Update global properties
                        propDataReference.child(selectedProperty.getmID()).
                                child(ShowPropertiesActivity.PROPERTY_INVEST_TOTAL_KEY).
                                setValue(globalPropertyOwned - investSellAmountPercent);

                        // Update current properties incomeBenefits
                        double newCashBenefits = selectedProperty.getmIncomeBenefits() - (selectedProperty.getmIncomeBenefits() /
                                ((selectedProperty.getmPercentageOwned() * 100) / (investSellAmountPercent * 100)));
                        userReference.child(displayName).child(MainActivity.USER_MY_PROPERTIES_KEY).
                                child(selectedProperty.getmName()).child(MainActivity.
                                USER_PROP_CASH_BENEFITS_AMOUNT).setValue(newCashBenefits);

                        // Update user cash (Varies a tiny bit due to database storage precision error on Google's End)
                        double newUserCash = (userCashValue + getRefundAmount());
                        userReference.child(displayName).child(MainActivity.USER_CASH_KEY)
                                .setValue(newUserCash);

                        // Update user property value total
                        userReference.child(displayName).
                                child(MainActivity.USER_PROPERTY_TOTAL_VALUE).
                                setValue(userTotalPropertyValue -
                                        (costAmount * (investSellAmountPercent)));
                    } else if (investSellAmountPercent == userPropOwnedAmount) {
                        Log.i(TAG, "onDataChange: " + investSellAmountPercent + " : " + userPropOwnedAmount);
                        // User wants to sell the whole property (not just the shares of it).
                        if (investSellAmountPercent < 0) {
                            Log.e(TAG, "onDataChange: Do nothing...User trying to sell " +
                                    "negative percentage of their property");
                            return;
                        }
                        // Sell the whole property
                        sellWholeProperty();
                    } else {
                        Toast.makeText(context.getApplicationContext(),
                                "Error: You are trying to sell more than you have...",
                                Toast.LENGTH_LONG).show();
                        Log.e(TAG, "onDataChange: User trying to sell invalid amount of their property" );
                        return;
                    }

                    // Let user know that everything is finished successfully
                    Toast.makeText(context.getApplicationContext(),
                            "Property shares sold successfully for $" + getRefundAmount() + "!",
                            Toast.LENGTH_LONG).show();

                    // Let the calling activity know that we want to exit (After sale)
                    fireWantToExit();
                } else {
                    Toast.makeText(context.getApplicationContext(),
                            "Property could not be sold because you do not own it..",
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "onDataChange: User trying to sell property they do not own..");

                    // Let caller know there has been an error and we want to exit
                    fireWantToExit();
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
     * Sell the entire selectedProperty for the user.
     */
    private void sellWholeProperty() {
        // Remove property from users list
        userReference.child(displayName).child(MainActivity
                .USER_MY_PROPERTIES_KEY).child(selectedProperty.getmName()).removeValue();

        // Refund user cash for the property
        userReference.child(displayName).
                child(MainActivity.USER_CASH_KEY).setValue(userCashValue + getRefundAmount());

        // Update users total property value
        userReference.child(displayName).
                child(MainActivity.USER_PROPERTY_TOTAL_VALUE).
                setValue(userTotalPropertyValue -
                        (costAmount * (investSellAmountPercent)));

        // Restore invested amount to the global property
        propDataReference.child(selectedProperty.getmID()).
                child(ShowPropertiesActivity.PROPERTY_INVEST_TOTAL_KEY).
                setValue(globalPropertyOwned - investSellAmountPercent);
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
                                if (investSellAmountPercent <= (1 - selectedProperty.getmInvestAmount())) {
                                    userReference.child(displayName).child(MainActivity.USER_CASH_KEY)
                                            .setValue((userCashValue - costAmount));

                                    // Update user property value holding
                                    userReference.child(displayName).child(MainActivity.USER_PROPERTY_TOTAL_VALUE)
                                            .setValue(userPropertyValue + (selectedProperty.getmCost()
                                                    * investSellAmountPercent));

                                    // Check if the user already owns shares in the selected property
                                    if (!ownsAlready) {
                                        // Update user properties to include key of new property invested in
                                        userReference.getParent().child(MainActivity.USER_LIST_KEY_PARENT).
                                                child(displayName).child(MainActivity.USER_MY_PROPERTIES_KEY)
                                                .child(selectedProperty.getmName()).setValue(selectedProperty.getmID());

                                        // Update GLOBAL investAmount on property (NOT OWNED)
                                        userReference.getParent().child(MainActivity.PROPERTY_DB_REF_KEY).
                                                child(selectedProperty.getmID()).
                                                child(ShowPropertiesActivity.PROPERTY_INVEST_TOTAL_KEY).
                                                setValue(selectedProperty.getmInvestAmount() +
                                                        investSellAmountPercent);

                                        // Add The amount owned to the property item.
                                        userReference.getParent().child(MainActivity.USER_LIST_KEY_PARENT).
                                                child(displayName).child(MainActivity.USER_MY_PROPERTIES_KEY)
                                                .child(selectedProperty.getmName()).
                                                child(MainActivity.USER_PROP_OWNED_AMOUNT).
                                                setValue(investSellAmountPercent);
                                    } else {
                                        try {
                                            // If the user does own shares, get the amount owned and update
                                            double currentOwnedAmount = dataSnapshot.
                                                    child(MainActivity.USER_MY_PROPERTIES_KEY).
                                                    child(selectedProperty.getmName()).
                                                    child(MainActivity.USER_PROP_OWNED_AMOUNT).
                                                    getValue(Double.class);

                                            // Update User property owned amount
                                            userReference.getParent().child(MainActivity.USER_LIST_KEY_PARENT).
                                                    child(displayName).child(MainActivity.USER_MY_PROPERTIES_KEY)
                                                    .child(selectedProperty.getmName()).
                                                    child(MainActivity.USER_PROP_OWNED_AMOUNT).
                                                    setValue(currentOwnedAmount + investSellAmountPercent);

                                            // Update GLOBAL investAmount on property (IF OWNED)
                                                /* We use propInvestAmountGlobal to determine
                                                   whether the property is being invested more from
                                                   the newpropmanager or the mypropmanager */
                                            double propInvestAmountGlobal = selectedProperty.
                                                    getmPercentageOwned() == 0 ? selectedProperty.
                                                    getmInvestAmount() : selectedProperty.
                                                    getmPercentageOwned();
                                            userReference.getParent().child(MainActivity.PROPERTY_DB_REF_KEY).
                                                    child(selectedProperty.getmID()).
                                                    child(ShowPropertiesActivity.PROPERTY_INVEST_TOTAL_KEY).
                                                    setValue(propInvestAmountGlobal +
                                                            investSellAmountPercent);

                                            // Get the old benefits
                                            oldBenefits = dataSnapshot.
                                                    child(MainActivity.USER_MY_PROPERTIES_KEY).
                                                    child(selectedProperty.getmName()).
                                                    child(MainActivity.USER_PROP_CASH_BENEFITS_AMOUNT).getValue(Double.class);
                                        } catch (NullPointerException e) {
                                            Log.i(TAG, "onDataChange: Problem getting values " +
                                                    "from DB in InvestingAssistant");
                                        }
                                    }

                                    // Update cash benefits
                                    userReference.child(displayName)
                                            .child(MainActivity.USER_MY_PROPERTIES_KEY).
                                            child(selectedProperty.getmName())
                                            .child(MainActivity.USER_PROP_CASH_BENEFITS_AMOUNT).
                                            setValue(oldBenefits + calculateCashBenefits(costAmount));

                                    Toast.makeText(context.getApplicationContext(),
                                            "Investment successful!", Toast.LENGTH_LONG).show();
                                    Log.i(TAG, "investInProperty: User successfully invested in the selected property!");

                                    fireWantToExit();
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
     *
     * @param costAmount The cost of the property
     * @return CashBenefits amount...
     */
    public static double calculateCashBenefits(double costAmount) {
        // Calculate cash benefits and display
        return ((costAmount / 150) + (costAmount * 0.22)) / 12;
    }

    /**
     * Calculates the amount of cash to refund to the user based on the
     * property and shares amount they are selling
     *
     * @return The refund amount ($)
     */
    private double getRefundAmount() {
        return (costAmount * (investSellAmountPercent ) * (0.75));
    }

    /**
     * Sets up the close activity listener for closing the Activity only when needed operations are
     * completed.
     * @param listener The OnWantToCloseListener
     */
    public void setWantToExitListener(OnWantToExitListener listener){
        this.exitListener = listener;
    }

    /**
     * Triggered when the application wants to exit the Activity
     */
    private void fireWantToExit(){
        if(this.exitListener != null)
            exitListener.onWantToExit();
    }

    /**
     * Rounds the given number to 2 decimal precision
     * @param n The number to round
     * @return The newly rounded number
     */
    private Double roundNumber(Double n) {
        String temp = String.format("%.2f", n);
        return Double.parseDouble(temp);
    }

}
