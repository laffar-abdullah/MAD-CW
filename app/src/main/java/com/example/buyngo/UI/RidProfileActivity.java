package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.buyngo.R;

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

        // Profile is part of rider-only flow, so enforce active session.
        if (!RiderSessionStore.isLoggedIn(this)) {
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.rid_profile);

        txtProfileNameHeader = findViewById(R.id.txtProfileNameHeader);
        txtFullNameValue = findViewById(R.id.txtFullNameValue);
        txtEmailValue = findViewById(R.id.txtEmailValue);
        txtPhoneValue = findViewById(R.id.txtPhoneValue);
        txtVehicleValue = findViewById(R.id.txtVehicleValue);
        txtTotalDeliveriesValue = findViewById(R.id.txtTotalDeliveriesValue);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Bottom navigation shortcuts link back to dashboard, history, and reviews.
        findViewById(R.id.navDashboard).setOnClickListener(v ->
            startActivity(new Intent(this, RidDashboardActivity.class)));
        findViewById(R.id.navHistory).setOnClickListener(v ->
            startActivity(new Intent(this, RidDeliveryHistoryActivity.class)));
        findViewById(R.id.navReviews).setOnClickListener(v ->
            startActivity(new Intent(this, RidReviewsActivity.class)));

        bindRiderData();

        // Logout -> back to Welcome screen, clear back stack
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            RiderSessionStore.clearSession(this);
            Intent intent = new Intent(this, AuthWelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void bindRiderData() {
        // Populate profile card from persisted rider session data.
        RiderSessionStore.RiderProfile profile = RiderSessionStore.getCurrentRider(this);
        if (profile == null) {
            return;
        }

        // Delivery count comes from the same rider-scoped history used in Delivery History.
        int completedDeliveries = OrderStatusStore.getDeliveryHistory(this).size();
        txtProfileNameHeader.setText(profile.name);
        txtFullNameValue.setText(profile.name);
        txtEmailValue.setText(profile.email);
        txtPhoneValue.setText(profile.phone);
        txtVehicleValue.setText(profile.vehicle);
        txtTotalDeliveriesValue.setText(completedDeliveries + " Deliveries");
    }
}
