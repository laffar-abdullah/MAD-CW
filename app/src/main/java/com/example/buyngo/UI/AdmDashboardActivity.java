package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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

        findViewById(R.id.btnManageProducts).setOnClickListener(v -> 
            startActivity(new Intent(this, AdmProductManagementActivity.class)));

        findViewById(R.id.btnViewProduct).setOnClickListener(v -> {
            // Logic for view product
        });

        findViewById(R.id.btnManageOrders).setOnClickListener(v -> {
            // Logic for manage orders
        });

        findViewById(R.id.btnViewFeedbacks).setOnClickListener(v -> {
            // Logic for view feedbacks
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