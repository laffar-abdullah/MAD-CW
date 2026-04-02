package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;

public class CusTrackingActivity extends AppCompatActivity {
    private TextView txtDeliveryStatus;
    private Button receivedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_tracking);

        txtDeliveryStatus = findViewById(R.id.txtDeliveryStatus);
        receivedButton = findViewById(R.id.receivedButton);

        refreshTrackingStatus();

        // Go to Feedback screen after receiving order
        receivedButton.setOnClickListener(v -> {
            startActivity(new Intent(this, CusFeedbackActivity.class));
        });
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

        // "Received" is enabled only once rider marks the order as delivered.
        boolean canConfirmReceived = OrderStatusStore.STATUS_DELIVERED.equals(status);
        receivedButton.setEnabled(canConfirmReceived);
        receivedButton.setAlpha(canConfirmReceived ? 1f : 0.5f);
    }
}