package ca.sykesdev.sheridango;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import model.InvestingAssistant;
import model.Property;

public class MyPropertyManager extends AppCompatActivity {

    // Activity Constants
    private final String TAG = "MY_PROP_MANAGER";

    // Declare control variables
    private ImageView imgPropertyImage;
    private TextView txtAboutProperty, txtInvestCostAmount, txtSellGainAmount;
    private EditText txtInvestMorePercent, txtSellPercent;
    private Button btnInvest, btnSellShares;

    // Property variable
    private Property selectedProperty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_property_manager);

        // Initiate control variables
        imgPropertyImage = findViewById(R.id.imgPropertyPhoto);
        txtAboutProperty = findViewById(R.id.txtAboutProperty);
        txtInvestCostAmount = findViewById(R.id.txtCashCostValue);
        txtSellGainAmount = findViewById(R.id.txtSellCashAmount);
        txtInvestMorePercent = findViewById(R.id.txtInvestMorePercent);
        txtSellPercent = findViewById(R.id.txtSellPercentage);
        btnInvest = findViewById(R.id.btnInvestMore);
        btnSellShares = findViewById(R.id.btnSellShares);

        // Get selected property
        Intent propIntent = getIntent();
        selectedProperty = propIntent.getParcelableExtra(MyPropertiesActivity
                .SELECTED_PROPERTY_INTENT_KEY);

        /**
         * Invest more in the property
         */
        btnInvest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        /**
         * Sell shares of the selected property
         */
        btnSellShares.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* TODO: Fix major errors that occur here.. (with global properties and with
                 local being completely sold at any selected percentage and global being able
                 to be sold multiple times to make global owned percent HUGE negative numbers that
                 make no sense.
                */
                if (false) {
                    double sellPercent = Double.parseDouble(txtSellPercent.getText().toString());

                    // Make sure the user entered a valid sell percent
                    if (sellPercent < 100 && sellPercent > 0) {
                        // Get an assistant to help with selling
                        InvestingAssistant investingAssistant =
                                new InvestingAssistant(selectedProperty, selectedProperty.getmIncomeBenefits(),
                                        selectedProperty.getmCost(),
                                        Double.parseDouble(txtSellPercent.getText().toString()),
                                        getApplicationContext());

                        // Sell the property
                        investingAssistant.sellSharesFromProperty();

                        // Exit the activity
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        txtSellPercent.setError(String.format("Error: Enter a number between 1 and %d",
                                selectedProperty.getmPercentageOwned()));
                    }
                } else {
                    Log.e(TAG, "onClick: Selling function still broken.. this feature " +
                            "will not work");
                }
            }
        });
    }
}
