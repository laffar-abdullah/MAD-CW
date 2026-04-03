package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;

/**
 * RidLoginActivity — entry point for the rider flow.
 *
 * Shows an email / password form and delegates authentication to
 * {@link RiderSessionStore}.  On success the rider is taken to
 * {@link RidDashboardActivity} and this activity is finished so the back
 * button does not return to the login screen.
 *
 * ── CHANGES FROM ORIGINAL ──────────────────────────────────────────────────
 *  • No logic changes needed — this activity was already correct.
 *  • Added comments explaining each key decision.
 * ───────────────────────────────────────────────────────────────────────────
 */
public class RidLoginActivity extends AppCompatActivity {

    private EditText riderEmailEditText;
    private EditText riderPasswordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Skip login when a rider session is already active (e.g. app restart).
        if (RiderSessionStore.isLoggedIn(this)) {
            startActivity(new Intent(this, RidDashboardActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.rid_login);

        riderEmailEditText    = findViewById(R.id.riderEmailEditText);
        riderPasswordEditText = findViewById(R.id.riderPasswordEditText);

        // Back arrow returns to the Welcome / role-selection screen.
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.riderLoginButton).setOnClickListener(v -> handleLogin());
    }

    /** Validates the form then delegates authentication to RiderSessionStore. */
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
        RiderSessionStore.RiderProfile profile =
                RiderSessionStore.authenticate(email, password);

        if (profile == null) {
            Toast.makeText(this, "Invalid rider credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        // Persist session and seed default order data so the dashboard always
        // has a delivery task to display on first login.
        RiderSessionStore.saveSession(this, profile);
        OrderStatusStore.initializeDefaultsIfMissing(this);

        startActivity(new Intent(this, RidDashboardActivity.class));
        finish();
    }
}
