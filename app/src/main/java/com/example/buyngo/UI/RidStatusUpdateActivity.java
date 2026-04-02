package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;

public class RidStatusUpdateActivity extends AppCompatActivity {
    private TextView txtOrderNumber;
    private TextView txtCurrentStatus;
    private android.view.View btnPickedUp;
    private android.view.View btnOutForDelivery;
    private android.view.View btnDelivered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Prevent direct access when rider is not authenticated.
        if (!RiderSessionStore.isLoggedIn(this)) {
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.rid_status_update);

        txtOrderNumber = findViewById(R.id.txtOrderNumber);
        txtCurrentStatus = findViewById(R.id.txtCurrentStatus);

        btnPickedUp = findViewById(R.id.btnPickedUp);
        btnOutForDelivery = findViewById(R.id.btnOutForDelivery);
        btnDelivered = findViewById(R.id.btnDelivered);

        // Show current order/status before any action is taken.
        refreshUi();

        // Each button writes a different status into OrderStatusStore.
        btnPickedUp.setOnClickListener(v ->
                saveStatus(OrderStatusStore.STATUS_PICKED_UP, false));
        btnOutForDelivery.setOnClickListener(v ->
                saveStatus(OrderStatusStore.STATUS_ON_THE_WAY, false));
        btnDelivered.setOnClickListener(v ->
                saveStatus(OrderStatusStore.STATUS_DELIVERED, true));
    }

    private void saveStatus(String status, boolean closeAfterUpdate) {
        // Store validates business flow and blocks invalid jumps.
        boolean updated = OrderStatusStore.updateStatus(this, status);
        if (!updated) {
            Toast.makeText(this, "Follow the status order: Awaiting Pickup -> Picked Up -> On the Way -> Delivered", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "Status updated: " + status, Toast.LENGTH_SHORT).show();
        refreshUi();
        if (closeAfterUpdate) {
            finish();
        }
    }

    private void refreshUi() {
        // Keep header and action buttons in sync with the same rider-scoped order.
        OrderStatusStore.OrderInfo order = OrderStatusStore.getCurrentOrder(this);
        txtOrderNumber.setText("Order: #" + order.orderId);
        txtCurrentStatus.setText("Current status: " + order.status);

        setButtonState(btnPickedUp, "Awaiting Pickup".equals(order.status));
        setButtonState(btnOutForDelivery, OrderStatusStore.STATUS_PICKED_UP.equals(order.status));
        setButtonState(btnDelivered, OrderStatusStore.STATUS_ON_THE_WAY.equals(order.status));
    }

    private void setButtonState(android.view.View button, boolean enabled) {
        // Disabled buttons indicate the next valid step clearly.
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.45f);
    }
}