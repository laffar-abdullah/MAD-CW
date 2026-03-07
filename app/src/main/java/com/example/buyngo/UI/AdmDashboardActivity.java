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

        // Navigate to Product Management
        findViewById(R.id.btnManageProducts).setOnClickListener(v -> {
            startActivity(new Intent(this, AdmProductManagementActivity.class));
        });

        // Navigate to Order Management
        findViewById(R.id.btnManageOrders).setOnClickListener(v -> {
            startActivity(new Intent(this, AdmOrderManagementActivity.class));
        });

        // Register a new rider account
        findViewById(R.id.btnRegisterRider).setOnClickListener(v ->
                startActivity(new Intent(this, AdmRegisterRiderActivity.class)));

        // Logout -> Welcome screen
        findViewById(R.id.btnAdminLogout).setOnClickListener(v -> {
            Intent intent = new Intent(this, AuthWelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}