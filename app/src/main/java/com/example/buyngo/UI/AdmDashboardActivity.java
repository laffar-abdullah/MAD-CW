package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.buyngo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;

public class AdmDashboardActivity extends AppCompatActivity {

    private DatabaseReference db;
    private FirebaseAuth mAuth;

    private TextView tvTotalProducts, tvTotalOrders, tvPendingOrders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adm_dashboard);

        db    = FirebaseDatabase.getInstance("https://buyngo-5b43e-default-rtdb.firebaseio.com/").getReference();
        mAuth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvTotalProducts = findViewById(R.id.tvTotalProducts);
        tvTotalOrders   = findViewById(R.id.tvTotalOrders);
        tvPendingOrders = findViewById(R.id.tvPendingOrders);

        findViewById(R.id.btnManageProducts).setOnClickListener(v ->
                startActivity(new Intent(this, AdmProductManagementActivity.class)));

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

        // Pending orders (status == "Ordered")
        db.child("orders").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long pendingCount = 0;
                for (DataSnapshot order : snapshot.getChildren()) {
                    String status = order.child("status").getValue(String.class);
                    if ("Ordered".equals(status)) pendingCount++;
                }
                if (tvPendingOrders != null)
                    tvPendingOrders.setText(String.valueOf(pendingCount));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        mAuth.signOut();
        Intent intent = new Intent(this, AuthWelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}