package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;

public class AdmDashboardActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adm_dashboard);

        // Manage Products
        findViewById(R.id.btnManageProducts).setOnClickListener(v -> {
            // startActivity(new Intent(this, AdmProductManagementActivity.class));
        });

        // Manage Orders
        findViewById(R.id.btnManageOrders).setOnClickListener(v ->
                startActivity(new Intent(this, AdmOrderManagementActivity.class)));

        // Register Rider
        findViewById(R.id.btnRegisterRider).setOnClickListener(v ->
                startActivity(new Intent(this, AdmRegisterRiderActivity.class)));

        // Assign Rider
        findViewById(R.id.btnAssignRider).setOnClickListener(v -> {
            // startActivity(new Intent(this, AdmAssignRiderActivity.class));
        });

        // View Feedbacks
        findViewById(R.id.btnViewFeedbacks).setOnClickListener(v -> {
            // startActivity(new Intent(this, AdmViewFeedbacksActivity.class));
        });

        // Logout
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            Intent intent = new Intent(this, AuthWelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
