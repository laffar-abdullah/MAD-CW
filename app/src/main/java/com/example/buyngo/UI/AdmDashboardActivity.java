package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
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

        findViewById(R.id.btnManageProducts).setOnClickListener(v ->
            startActivity(new Intent(this, AdmProductManagementActivity.class)));

        findViewById(R.id.btnManageOrders).setOnClickListener(v -> {
            // Logic for manage orders
        });

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            // Logic for logout
        });
    }
}
