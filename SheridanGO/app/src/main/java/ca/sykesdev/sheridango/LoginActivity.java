package ca.sykesdev.sheridango;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private final String TAG = "LOGIN_ACTIVITY";

    // Declare controls
    private EditText txtDisplayName;
    private Button btnSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Init Controls
        txtDisplayName = findViewById(R.id.txtDisplayName);
        btnSignIn = findViewById(R.id.btnLogin);

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String displayName = txtDisplayName.getText().toString();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");

                // Check for valid display name
                if (displayName.isEmpty() || displayName.equalsIgnoreCase("NULL")) {
                    txtDisplayName.setError("Error: Please enter a valid display name...");
                    Log.e(TAG, "onClick: User entered an invalid display name value..");
                    return;
                }

                /**
                 * Used to check if the username already exists in the game...
                 * Handled accordingly
                 */
                ref.child(displayName).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Username already exists
                            txtDisplayName.setError("Error: Name already taken by another user!" +
                                    " Please try again");
                            Log.e(TAG, "onClick: User tried to user name that was already taken...");
                        } else {
                            // Name is unique, continue with user creation..
                            Log.i(TAG, "onDataChange: Name unique... Creating user!");
                            Intent displayNameIntent = new Intent();
                            displayNameIntent.putExtra(MainActivity.DISPLAY_NAME_KEY, displayName);

                            setResult(RESULT_FIRST_USER, displayNameIntent);
                            finish(); // Go back to the Dashboard
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
        });
    }
}
