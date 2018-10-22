package ca.sykesdev.sheridangoapp;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class DashboardActivity extends Activity {

    // Default class requestCode
    public static final int MAIN_ACTIVITY = 0;

    // Places object needed
    private PlaceDetectionClient placeDetectionClient;

    // Control variables
    private TextView txtAddress;
    private Button btnTestPlaces;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Import controls
        txtAddress = (TextView) findViewById(R.id.txtAddress);
        btnTestPlaces = (Button) findViewById(R.id.btnTestLocation);

        // Start by instantiating the places API object we will be using to get current location
        placeDetectionClient = Places.getPlaceDetectionClient(this, null);

        checkPermissions();

        btnTestPlaces.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                        android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(DashboardActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MAIN_ACTIVITY);
                    return;
                }
                Task<PlaceLikelihoodBufferResponse> placeResult =
                        placeDetectionClient.getCurrentPlace(null);

                // Get the place nearby current location
                placeResult.addOnCompleteListener(new OnCompleteListener<PlaceLikelihoodBufferResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<PlaceLikelihoodBufferResponse> task) {
                        if (task.isSuccessful()) {
                            PlaceLikelihoodBufferResponse likelyPlaces = task.getResult();
                            float maxLikelihood = 0;
                            Place currentPlace = null;

                            for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                                if (maxLikelihood < placeLikelihood.getLikelihood()) {
                                    maxLikelihood = placeLikelihood.getLikelihood();
                                    currentPlace = placeLikelihood.getPlace();
                                }
                            }

                            if (currentPlace != null) {
                                txtAddress.setText(String.format(getString(R.string.address_text),
                                        currentPlace.getName(),
                                        currentPlace.getAddress(),
                                        System.currentTimeMillis()));

                            } else {
                                Toast toast = Toast.makeText(DashboardActivity.this,
                                        "Error: Failed to determine place",
                                        Toast.LENGTH_LONG);
                                toast.show();
                            }

                            likelyPlaces.release(); // Release the buffer after getting data
                        } else {
                            Toast toast = Toast.makeText(DashboardActivity.this,
                                    "Error: Failed to get current location using Places API call",
                                    Toast.LENGTH_LONG);
                            toast.show();
                        }
                    }
                });
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MAIN_ACTIVITY) {
            if (grantResults[0] == RESULT_CANCELED) {
                Toast toast = Toast.makeText(this,
                        "Error: App will fail. Need ACCESS_FINE_LOCATION permission",
                        Toast.LENGTH_LONG);
                toast.show();
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void checkPermissions() {

    }

}
