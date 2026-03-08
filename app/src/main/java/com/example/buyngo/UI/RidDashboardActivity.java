package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;

public class RidDashboardActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rid_dashboard);

        // Update Status button inside task card
        findViewById(R.id.btnUpdateStatus).setOnClickListener(v ->
                startActivity(new Intent(this, RidStatusUpdateActivity.class)));

        // My Reviews button
        findViewById(R.id.btnMyReviews).setOnClickListener(v ->
                startActivity(new Intent(this, RidReviewsActivity.class)));

        // Delivery History button
        findViewById(R.id.btnDeliveryHistory).setOnClickListener(v ->
                startActivity(new Intent(this, RidDeliveryHistoryActivity.class)));

        // My Profile button
        findViewById(R.id.btnRiderProfile).setOnClickListener(v ->
                startActivity(new Intent(this, RidProfileActivity.class)));

        // Logout -> Welcome screen
        findViewById(R.id.btnRiderLogout).setOnClickListener(v -> {
            Intent intent = new Intent(this, AuthWelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}