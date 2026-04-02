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
        findViewById(R.id.cardManageProducts).setOnClickListener(v -> {
            // Uncomment or create this activity when needed
            // startActivity(new Intent(this, AdmProductManagementActivity.class));
        });

        // Manage Orders
        findViewById(R.id.cardManageOrders).setOnClickListener(v ->
                startActivity(new Intent(this, AdmOrderManagementActivity.class)));

        // Register Rider
        findViewById(R.id.cardRegisterRider).setOnClickListener(v ->
                startActivity(new Intent(this, AdmRegisterRiderActivity.class)));

        // Logout
        findViewById(R.id.cardLogout).setOnClickListener(v -> {
            Intent intent = new Intent(this, AuthWelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
