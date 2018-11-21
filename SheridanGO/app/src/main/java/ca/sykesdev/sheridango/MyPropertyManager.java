package ca.sykesdev.sheridango;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
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

import ca.sykesdev.sheridango.model.InvestingAssistant;
import ca.sykesdev.sheridango.model.OnWantToExitListener;
import ca.sykesdev.sheridango.model.PhotoHelper;
import ca.sykesdev.sheridango.model.Property;

public class MyPropertyManager extends AppCompatActivity {

    // Activity Constants
    private final String TAG = "MY_PROP_MANAGER";

    // Declare control variables
    private ImageView imgPropertyImage;
    private TextView txtNameOfProperty, txtAboutProperty, txtInvestCostAmount, txtSellGainAmount;
    private EditText txtInvestMorePercent, txtSellPercent;
    private Button btnInvest, btnSellShares;

    // Activity variables
    private Property selectedProperty;
    private boolean imageSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_property_manager);

        // Initiate control variables
        imgPropertyImage = findViewById(R.id.imgPropertyPhoto);
        txtNameOfProperty = findViewById(R.id.txtNameOfProperty);
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
         * Set Text Change Listener for buying
         */
        txtInvestMorePercent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // DO NOTHING
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Check first to make sure we are working with something.
                if (!(txtInvestMorePercent.getText().toString().isEmpty())) {
                    double invAmountEntered = Double.parseDouble(txtInvestMorePercent.getText().toString());

                    // Update the cash cost based on the percent the user would like to buy
                    if (invAmountEntered > (100 - (selectedProperty.getmPercentageOwned() * 100)) || invAmountEntered < 1) {
                        txtInvestMorePercent.setError(String.format("Error: Enter percent value between 1-%.2f.",
                                100 - (selectedProperty.getmPercentageOwned() * 100)));
                    } else {
                        double costAmount = ((selectedProperty.getmCost() * (
                                invAmountEntered / 100))
                                * 1.100621224124 + 100);

                        // Update cost amount for user to see
                        costAmount = Math.round(costAmount * 100.0) / 100.0;
                        txtInvestCostAmount.setText(String.format(getString(R.string.txt_cost_text),
                                costAmount));

                        // Update cash benefits amount and Ownership amount
                        double benefitChange = InvestingAssistant.calculateCashBenefits(costAmount);
                        txtAboutProperty.setText(String.format(getString(R.string.
                                        txt_income_owned_label_myprop_manager_buy_preview),
                                selectedProperty.getmPercentageOwned() * 100, invAmountEntered,
                                selectedProperty.getmIncomeBenefits(), benefitChange));
                    }
                } else {
                    setPropertyValues();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // DO NOTHING
            }
        });

        /**
         * Invest more in the property
         */
        btnInvest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int CLOSE = 1;
                double investAmount = Double.parseDouble(txtInvestMorePercent.getText().toString());

                if (investAmount > (100 - (selectedProperty.getmPercentageOwned() * 100))
                        || investAmount < 0
                        || txtInvestMorePercent.getText().toString().length() == 0) {
                    Log.i(TAG, "investInProperty: Invalid input");
                    txtInvestMorePercent.setError("Error: Invalid input.");
                } else {
                    InvestingAssistant investingAssistant = new
                            InvestingAssistant(selectedProperty,
                            selectedProperty.getmIncomeBenefits(),
                            Double.parseDouble(txtInvestCostAmount.getText().toString().substring(7,
                                    txtInvestCostAmount.getText().length())),
                            (investAmount / 100),
                            getApplicationContext());


                    // Setup exit handler before we start selling operation
                    @SuppressLint("HandlerLeak") final Handler handler = new Handler() {
                        public void handleMessage(Message msg) {
                            if (msg.what == CLOSE) {
                                MyPropertyManager.this.setResult(RESULT_OK);
                                MyPropertyManager.this.finish();
                            }
                        }
                    };

                    // Set the listener
                    investingAssistant.setWantToExitListener(new OnWantToExitListener() {
                        @Override
                        public void onWantToExit() {
                            handler.sendEmptyMessage(CLOSE);
                        }
                    });

                    // Invest in the property
                    investingAssistant.investInProperty();
                }
            }
        });

        /**
         * Set Text Change Listener for selling
         */
        txtSellPercent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // DO NOTHING
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Check first to make sure we are working with something.
                if (!(txtSellPercent.getText().toString().isEmpty())) {
                    double sellAmountEntered = Double.parseDouble(txtSellPercent.getText().toString());

                    // Update the cash cost based on the percent the user would like to buy
                    if (sellAmountEntered > (selectedProperty.getmPercentageOwned() * 100)
                            || sellAmountEntered < 1) {
                        txtSellPercent.setError(String.format("Error: Enter a value between 1-%.1f",
                                (selectedProperty.getmPercentageOwned() * 100)));
                    } else {
                        double sellCashAmount = ((selectedProperty.getmCost() *
                                (sellAmountEntered / 100)) * 0.75);

                        // Update cost amount for user to see
                        sellCashAmount = Math.round(sellCashAmount * 100.0) / 100.0;
                        txtSellGainAmount.setText(String.format(getString(R.string.txt_sell_amount_text),
                                sellCashAmount));

                        // Update cash benefits amount and Ownership amount
                        double benefitChange = (selectedProperty.getmIncomeBenefits() *
                                (sellAmountEntered / selectedProperty.getmPercentageOwned())) / 100;
                        txtAboutProperty.setText(String.format(getString(R.string.
                                        txt_income_owned_label_myprop_manager_sell_preview),
                                selectedProperty.getmPercentageOwned() * 100, sellAmountEntered,
                                selectedProperty.getmIncomeBenefits(), benefitChange));
                    }
                } else {
                    setPropertyValues();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // DO NOTHING
            }
        });

        /**
         * Sell shares of the selected property
         */
        btnSellShares.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int CLOSE = 1; // number to identify what happens
                double sellPercent = Double.parseDouble(txtSellPercent.getText().toString());

                // Make sure the user entered a valid sell percent
                if (sellPercent <= (selectedProperty.getmPercentageOwned()*100) && sellPercent > 0) {
                    // Get an assistant to help with selling
                    InvestingAssistant investingAssistant =
                            new InvestingAssistant(selectedProperty, selectedProperty.getmIncomeBenefits(),
                                    selectedProperty.getmCost(),
                                    Double.parseDouble(txtSellPercent.getText().toString()),
                                    getApplicationContext());

                    // Setup exit handler before we start selling operation
                    @SuppressLint("HandlerLeak") final Handler handler = new Handler() {
                        public void handleMessage(Message msg) {
                            if (msg.what == CLOSE)
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
                    txtSellPercent.setError(String.format("Error: Enter a number between 1-%.1f",
                            (selectedProperty.getmPercentageOwned() * 100)));
                }
            }
        });
    }

    /**
     * Sets the proper values for the selectedProperty
     */
    private void setPropertyValues() {
        // Init all fields to correct values
        txtNameOfProperty.setText(String.format(getString(R.string.txt_myprop_name_text),
                selectedProperty.getmName()));
        txtAboutProperty.setText(String.format(getString(R.string.
                        txt_income_owned_label_myprop_manager),
                selectedProperty.getmPercentageOwned() * 100, selectedProperty.getmIncomeBenefits()));
        txtInvestCostAmount.setText(String.format(getString(R.string.txt_cost_text),
                0.0));
        txtSellGainAmount.setText(String.format(getString(R.string.txt_sell_amount_text),
                0.0));

        // Get image data for property photo
        if (selectedProperty.getmPhotoID().equalsIgnoreCase("NULL")) {
            imgPropertyImage.setImageDrawable(getDrawable(R.drawable.photo_not_found));
        } else {
            if (!imageSet) {
                PhotoSyncTask getPhotoTask = new PhotoSyncTask();
                getPhotoTask.execute();
            }
        }
    }


    private class PhotoSyncTask extends AsyncTask<Void, Void, Bitmap> {

        @Override
        protected void onPreExecute() {
            Log.i(TAG, "onPreExecute: Getting photo from API");
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            PhotoHelper helper = new PhotoHelper(selectedProperty.getmPhotoID());
            return helper.getPhotoFromID();
        }

        @Override
        protected void onPostExecute(Bitmap imageBitmap) {
            Log.i(TAG, "onPostExecute: Received photo. Placing it in imageView...");
            imgPropertyImage.setImageBitmap(imageBitmap);
            imageSet = true;
        }
    }
}
