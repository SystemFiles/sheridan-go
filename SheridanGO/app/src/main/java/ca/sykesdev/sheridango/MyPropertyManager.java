package ca.sykesdev.sheridango;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.FirebaseDatabase;

import model.InvestingAssistant;
import model.OnWantToExitListener;
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
        txtInvestCostAmount = findViewById(R.id.txtCashCost);
        txtSellGainAmount = findViewById(R.id.txtSellCashAmount);
        txtInvestMorePercent = findViewById(R.id.txtInvestMorePercent);
        txtSellPercent = findViewById(R.id.txtSellPercentage);
        btnInvest = findViewById(R.id.btnInvestMore);
        btnSellShares = findViewById(R.id.btnSellShares);

        // Get selected property
        Intent propIntent = getIntent();
        selectedProperty = propIntent.getParcelableExtra(MyPropertiesActivity
                .SELECTED_PROPERTY_INTENT_KEY);

        // Initiate control values with selected property values
        setPropertyValues();

        // Setup Listeners
        /**
         * Invest more in the property
         */
        btnInvest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Use InvestingAssistant to help with investing more in a property
            }
        });

        /**
         * Sell shares of the selected property
         */
        btnSellShares.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* TODO: Keep an eye on this, still needs more testing*/
                final int CLOSE = 1; // number to identify what happens

                if (true) {
                    double sellPercent = Double.parseDouble(txtSellPercent.getText().toString());

                    // Make sure the user entered a valid sell percent
                    if (sellPercent < 100 && sellPercent > 0) {
                        // Get an assistant to help with selling
                        InvestingAssistant investingAssistant =
                                new InvestingAssistant(selectedProperty, selectedProperty.getmIncomeBenefits(),
                                        selectedProperty.getmCost(),
                                        Double.parseDouble(txtSellPercent.getText().toString()),
                                        getApplicationContext());

                        // Setup exit handler before we start selling operation
                        @SuppressLint("HandlerLeak") final Handler handler = new Handler(){
                            public void handleMessage(Message msg){
                                if(msg.what == CLOSE)
                                    MyPropertyManager.this.setResult(RESULT_OK);
                                    MyPropertyManager.this.finish();
                            }
                        };

                        // Set the listener
                        investingAssistant.setWantToExitListener(new OnWantToExitListener() {
                            @Override
                            public void onWantToExit() {
                                handler.sendEmptyMessage(CLOSE);
                            }
                        });

                        // {PERFORM}: Sell the property
                        investingAssistant.sellSharesFromProperty();
                    } else {
                        txtSellPercent.setError(String.format("Error: Enter a number between 1 and %.2f",
                                selectedProperty.getmPercentageOwned()));
                    }
                } else {
                    Log.e(TAG, "onClick: Selling function still broken.. this feature " +
                            "will not work");
                }
            }
        });
    }

    /**
     * Sets the proper values for the selectedProperty
     */
    private void setPropertyValues() {
        // Init all fields to correct values
        txtAboutProperty.setText(String.format(getString(R.string.
                        txt_income_owned_label_myprop_manager),
                selectedProperty.getmPercentageOwned(), selectedProperty.getmIncomeBenefits()));
        txtInvestCostAmount.setText(String.format(getString(R.string.txt_cost_text),
                0.0));
        txtSellGainAmount.setText(String.format(getString(R.string.txt_sell_amount_text),
                0.0));
    }
}
