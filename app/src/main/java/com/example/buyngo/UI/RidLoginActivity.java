package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;

public class RidLoginActivity extends AppCompatActivity {

    private EditText riderEmailEditText;
    private EditText riderPasswordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Skip login when a rider session is already active.
        if (RiderSessionStore.isLoggedIn(this)) {
            startActivity(new Intent(this, RidDashboardActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.rid_login);

        riderEmailEditText = findViewById(R.id.riderEmailEditText);
        riderPasswordEditText = findViewById(R.id.riderPasswordEditText);

        // Back to Welcome screen
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.riderLoginButton).setOnClickListener(v -> handleLogin());
    }

    private void handleLogin() {
        // Read the latest values from the login form.
        String email = riderEmailEditText.getText().toString().trim();
        String password = riderPasswordEditText.getText().toString();

        // Basic client-side validation before authentication.
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

        // Verify rider against local demo accounts.
        RiderSessionStore.RiderProfile profile = RiderSessionStore.authenticate(email, password);
        if (profile == null) {
            Toast.makeText(this, "Invalid rider credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        // Persist session and seed default order data for rider flow.
        RiderSessionStore.saveSession(this, profile);
        OrderStatusStore.initializeDefaultsIfMissing(this);
        startActivity(new Intent(this, RidDashboardActivity.class));
        finish();
    }
}