package ca.sykesdev.sheridango;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import model.PhotoHelper;
import model.Property;

public class NewPropertyManager extends AppCompatActivity {

    // Declare control variables
    private TextView txtNameOfProperty, txtPropertyInfo, txtCashCostInfo;
    private EditText txtEnterInvestAmount;
    private ImageView imgPropertyPhoto;
    private Button btnBuy;

    // Activity Constants and Variables
    private final String TAG = "NEW_PROPERTY_MANAGER";
    private Property selectedProperty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_property_manager);

        // Get information from previous activity
        Intent intent = getIntent();
        selectedProperty =
                intent.getParcelableExtra(ShowPropertiesActivity.PROPERTY_LIST_INTENT_KEY);

        // Initialize Control Variables
        txtNameOfProperty = findViewById(R.id.txtNameOfProperty);
        txtPropertyInfo = findViewById(R.id.txtPropertyInfo);
        txtCashCostInfo = findViewById(R.id.txtCashCostValue);
        txtEnterInvestAmount = findViewById(R.id.txtBuyPercentage);
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
            imgPropertyPhoto.setImageDrawable(getDrawable(R.drawable.sheridan_go_logo));
        } else {
            // Set the image to a requested image from Google
            GetPhotoTask photoTask = new GetPhotoTask();
            photoTask.execute();
        }

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

                        costAmount = Math.round(costAmount * 100.0) / 100.0;
                        txtCashCostInfo.setText(String.format(getString(R.string.txt_cost_text), costAmount));
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // DO NOTHING
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
