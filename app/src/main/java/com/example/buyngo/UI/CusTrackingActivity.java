package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;

public class CusTrackingActivity extends AppCompatActivity {

    // This label shows the latest delivery status shared with the rider side.
    private TextView txtDeliveryStatus;

    // The customer uses this button to confirm that the parcel has arrived.
    private Button receivedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_tracking);

        // Connect the screen widgets so we can update the status text and button state.
        txtDeliveryStatus = findViewById(R.id.txtDeliveryStatus);
        receivedButton = findViewById(R.id.receivedButton);

        // Load the current delivery status as soon as the page opens.
        refreshTrackingStatus();

        // When the customer confirms receipt, we send them to the feedback screen.
        receivedButton.setOnClickListener(v -> startActivity(new Intent(this, CusFeedbackActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshTrackingStatus();
    }

    private void refreshTrackingStatus() {
        // Customer view mirrors the same shared status set by rider actions.
        String status = OrderStatusStore.getStatus(this);
        txtDeliveryStatus.setText("Status: " + status);

        // The confirm button only becomes active once the rider marks the order as delivered.
        boolean canConfirmReceived = OrderStatusStore.STATUS_DELIVERED.equals(status);
        receivedButton.setEnabled(canConfirmReceived);
        receivedButton.setAlpha(canConfirmReceived ? 1f : 0.5f);
    }
}