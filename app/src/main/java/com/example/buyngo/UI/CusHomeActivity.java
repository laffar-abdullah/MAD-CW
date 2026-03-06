package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CusHomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_home);

        // Profile icon -> Customer Profile
        ImageButton btnProfile = findViewById(R.id.btnProfile);
        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(this, CusProfileActivity.class)));

        // Product cards -> go to product detail
        Button addBtn1 = findViewById(R.id.addToCartBtn1);
        Button addBtn2 = findViewById(R.id.addToCartBtn2);
        Button addBtn3 = findViewById(R.id.addToCartBtn3);
        addBtn1.setOnClickListener(v -> startActivity(new Intent(this, CusProductDetailActivity.class)));
        addBtn2.setOnClickListener(v -> startActivity(new Intent(this, CusProductDetailActivity.class)));
        addBtn3.setOnClickListener(v -> startActivity(new Intent(this, CusProductDetailActivity.class)));

        // Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_cart) {
                startActivity(new Intent(this, CusCartActivity.class));
            } else if (id == R.id.nav_orders) {
                startActivity(new Intent(this, CusTrackingActivity.class));
            } else if (id == R.id.nav_reviews) {
                startActivity(new Intent(this, CusFeedbackActivity.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, CusProfileActivity.class));
            }
            return true;
        });
    }
}