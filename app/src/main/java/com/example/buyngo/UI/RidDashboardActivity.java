package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;

public class RidDashboardActivity extends AppCompatActivity {
    private TextView txtTaskOrderId;
    private TextView txtTaskCustomer;
    private TextView txtTaskAddress;
    private TextView txtTaskStatus;
    private View navHistory;
    private View navReviews;
    private View navProfile;
    private View sampleTaskCard;
    private View btnTaskUpdateStatus;
    private View txtNoActiveTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Rider screens are protected; redirect when session is missing.
        if (!RiderSessionStore.isLoggedIn(this)) {
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.rid_dashboard);

        txtTaskOrderId = findViewById(R.id.txtTaskOrderId);
        txtTaskCustomer = findViewById(R.id.txtTaskCustomer);
        txtTaskAddress = findViewById(R.id.txtTaskAddress);
        txtTaskStatus = findViewById(R.id.txtTaskStatus);
        sampleTaskCard = findViewById(R.id.sampleTaskCard);
        btnTaskUpdateStatus = findViewById(R.id.btnTaskUpdateStatus);
        txtNoActiveTask = findViewById(R.id.txtNoActiveTask);
        navHistory = findViewById(R.id.navHistory);
        navReviews = findViewById(R.id.navReviews);
        navProfile = findViewById(R.id.navProfile);

        // Ensure task data exists so dashboard always has a delivery to show.
        OrderStatusStore.initializeDefaultsIfMissing(this);

        // Task card and button both open the same status update screen.
        sampleTaskCard.setOnClickListener(v ->
                startActivity(new Intent(this, RidStatusUpdateActivity.class)));
        btnTaskUpdateStatus.setOnClickListener(v ->
                startActivity(new Intent(this, RidStatusUpdateActivity.class)));

        // Bottom navigation mirrors the three rider destinations for faster switching.
        navHistory.setOnClickListener(v -> startActivity(new Intent(this, RidDeliveryHistoryActivity.class)));
        navReviews.setOnClickListener(v -> startActivity(new Intent(this, RidReviewsActivity.class)));
        navProfile.setOnClickListener(v -> startActivity(new Intent(this, RidProfileActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Rebind task so status updates are reflected after returning.
        refreshTask();
    }

    private void refreshTask() {
        // Dashboard task card mirrors the current rider-scoped order state from storage.
        OrderStatusStore.OrderInfo order = OrderStatusStore.getCurrentOrder(this);

        boolean hasActiveTask = !OrderStatusStore.STATUS_DELIVERED.equals(order.status);
        sampleTaskCard.setVisibility(hasActiveTask ? View.VISIBLE : View.GONE);
        txtNoActiveTask.setVisibility(hasActiveTask ? View.GONE : View.VISIBLE);

        if (!hasActiveTask) {
            return;
        }

        txtTaskOrderId.setText("Order #" + order.orderId);
        txtTaskCustomer.setText("Customer: " + order.customerName);
        txtTaskAddress.setText("Address: " + order.customerAddress);
        txtTaskStatus.setText(order.status);
        applyStatusChipStyle(order.status);
    }

    private void applyStatusChipStyle(String status) {
        int colorRes;
        if (OrderStatusStore.STATUS_PICKED_UP.equals(status)) {
            colorRes = R.color.status_picked;
        } else if (OrderStatusStore.STATUS_ON_THE_WAY.equals(status)) {
            colorRes = R.color.status_out_for_delivery;
        } else {
            colorRes = R.color.status_ordered;
        }

        txtTaskStatus.setTextColor(getColor(colorRes));
    }
}
