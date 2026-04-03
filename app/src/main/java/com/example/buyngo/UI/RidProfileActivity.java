package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.buyngo.R;

/**
 * RidProfileActivity — displays the logged-in rider's personal details and
 * total completed deliveries, and provides a logout button.
 *
 * Profile data is read from {@link RiderSessionStore} (name, email, phone,
 * vehicle) and the delivery count from {@link OrderStatusStore#getDeliveryHistory}.
 *
 * ── CHANGES FROM ORIGINAL ──────────────────────────────────────────────────
 *  BUG FIX — The original navProfile bottom-nav item (the active tab on this
 *  screen) had a click listener that launched a NEW RidProfileActivity on top
 *  of the current one, stacking duplicates on the back stack every time the
 *  rider tapped "Profile" while already on the profile screen.
 *  FIX: the navProfile listener now calls bindRiderData() to simply refresh
 *  the displayed data in-place instead of starting another activity.
 *
 *  IMPROVEMENT — Added a null-guard before accessing profile fields in
 *  bindRiderData() so the app does not crash if the session is cleared from
 *  another thread between the isLoggedIn check and the actual data read.
 * ───────────────────────────────────────────────────────────────────────────
 */
public class RidProfileActivity extends AppCompatActivity {

    private TextView txtProfileNameHeader;
    private TextView txtFullNameValue;
    private TextView txtEmailValue;
    private TextView txtPhoneValue;
    private TextView txtVehicleValue;
    private TextView txtTotalDeliveriesValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Profile is part of the rider-only flow — enforce an active session.
        if (!RiderSessionStore.isLoggedIn(this)) {
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.rid_profile);

        // ── Toolbar ────────────────────────────────────────────────────────
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Back arrow returns to whichever screen launched the profile.
        toolbar.setNavigationOnClickListener(v -> finish());

        // ── View binding ───────────────────────────────────────────────────
        txtProfileNameHeader    = findViewById(R.id.txtProfileNameHeader);
        txtFullNameValue        = findViewById(R.id.txtFullNameValue);
        txtEmailValue           = findViewById(R.id.txtEmailValue);
        txtPhoneValue           = findViewById(R.id.txtPhoneValue);
        txtVehicleValue         = findViewById(R.id.txtVehicleValue);
        txtTotalDeliveriesValue = findViewById(R.id.txtTotalDeliveriesValue);

        // ── Bottom navigation ──────────────────────────────────────────────
        findViewById(R.id.navDashboard).setOnClickListener(v ->
                startActivity(new Intent(this, RidDashboardActivity.class)));

        findViewById(R.id.navHistory).setOnClickListener(v ->
                startActivity(new Intent(this, RidDeliveryHistoryActivity.class)));

        findViewById(R.id.navReviews).setOnClickListener(v ->
                startActivity(new Intent(this, RidReviewsActivity.class)));

        // BUG FIX: navProfile was launching a new RidProfileActivity and
        // stacking duplicates.  Now just refreshes the current screen.
        findViewById(R.id.navProfile).setOnClickListener(v -> bindRiderData());

        // ── Populate profile data ──────────────────────────────────────────
        bindRiderData();

        // ── Logout ─────────────────────────────────────────────────────────
        // Clears the session and sends the rider back to the Welcome screen,
        // clearing the entire back stack so they cannot press Back to return.
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            RiderSessionStore.clearSession(this);
            Intent intent = new Intent(this, AuthWelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    /**
     * Reads the rider's saved profile from the session store and fills in all
     * the text views on the card.
     *
     * The delivery count is pulled from the rider-scoped history list so it
     * matches exactly what is shown on the Delivery History screen.
     *
     * IMPROVEMENT: null-guard on profile prevents a NullPointerException in
     * the unlikely event the session is cleared between the isLoggedIn check
     * and this read.
     */
    private void bindRiderData() {
        RiderSessionStore.RiderProfile profile = RiderSessionStore.getCurrentRider(this);

        if (profile == null) {
            // Session was cleared unexpectedly — send to login.
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        // Delivery count mirrors the rider-scoped history used in History screen.
        int completedDeliveries = OrderStatusStore.getDeliveryHistory(this).size();

        txtProfileNameHeader.setText(profile.name);
        txtFullNameValue.setText(profile.name);
        txtEmailValue.setText(profile.email);
        txtPhoneValue.setText(profile.phone);
        txtVehicleValue.setText(profile.vehicle);
        txtTotalDeliveriesValue.setText(completedDeliveries + " Deliveries");
    }
}
