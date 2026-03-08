package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;

public class AdmDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adm_dashboard);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
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

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            Intent intent = new Intent(AdmDashboardActivity.this, AdmLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
