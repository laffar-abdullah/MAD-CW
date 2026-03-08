package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;

public class AdmDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adm_dashboard);

        Button btnManageProducts = findViewById(R.id.btnManageProducts);
        Button btnManageOrders = findViewById(R.id.btnManageOrders);
        Button btnRegisterRider = findViewById(R.id.btnRegisterRider);
        Button btnLogout = findViewById(R.id.btnLogout);

        btnManageProducts.setOnClickListener(v -> {
            // Intent intent = new Intent(AdmDashboardActivity.this, AdmProductManagementActivity.class);
            // startActivity(intent);
        });

        btnManageOrders.setOnClickListener(v -> {
            Intent intent = new Intent(AdmDashboardActivity.this, AdmOrderManagementActivity.class);
            startActivity(intent);
        });

        btnRegisterRider.setOnClickListener(v -> {
            Intent intent = new Intent(AdmDashboardActivity.this, AdmRegisterRiderActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(AdmDashboardActivity.this, AuthWelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
