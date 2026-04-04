package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;


public class RidLoginActivity extends AppCompatActivity {

    private EditText riderEmailEditText;
    private EditText riderPasswordEditText;
    private Button riderLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.rid_login);

        riderEmailEditText    = findViewById(R.id.riderEmailEditText);
        riderPasswordEditText = findViewById(R.id.riderPasswordEditText);
        riderLoginButton      = findViewById(R.id.riderLoginButton);

        // Back arrow returns to the Welcome / role-selection screen.
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        riderLoginButton.setOnClickListener(v -> handleLogin());
    }

    
    private void handleLogin() {
        String email    = riderEmailEditText.getText().toString().trim();
        String password = riderPasswordEditText.getText().toString();

        // Client-side validation: reject before hitting the store.
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            riderEmailEditText.setError("Enter a valid email");
            riderEmailEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            riderPasswordEditText.setError("Enter your password");
            riderPasswordEditText.requestFocus();
            return;
        }

        // Verify credentials against the known rider accounts.
        riderLoginButton.setEnabled(false);

        FirebaseRiderRepository.authenticateRider(email, password,
                new FirebaseRiderRepository.ResultCallback<FirebaseRiderRepository.RiderAccount>() {
                    @Override
                    public void onSuccess(FirebaseRiderRepository.RiderAccount account) {
                        String vehicleDisplay = account.vehicle == null ? "Motorbike" : account.vehicle;
                        if (!TextUtils.isEmpty(account.vehicleNumber)) {
                            vehicleDisplay = vehicleDisplay + " - " + account.vehicleNumber;
                        }

                        RiderSessionStore.RiderProfile profile = new RiderSessionStore.RiderProfile(
                                account.name,
                                account.email,
                                account.phone,
                            vehicleDisplay,
                            account.profileImageUrl);

                        RiderSessionStore.saveSession(RidLoginActivity.this, profile);
                        OrderStatusStore.initializeDefaultsIfMissing(RidLoginActivity.this);
                        riderLoginButton.setEnabled(true);
                        startActivity(new Intent(RidLoginActivity.this, RidDashboardActivity.class));
                        finish();
                    }

                    @Override
                    public void onError(String message) {
                        riderLoginButton.setEnabled(true);
                        Toast.makeText(RidLoginActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

