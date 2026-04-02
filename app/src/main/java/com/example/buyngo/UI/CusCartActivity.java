package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;

public class CusCartActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_cart);

        // The checkout button moves the customer to the checkout screen.
        findViewById(R.id.checkoutButton).setOnClickListener(v -> {
            startActivity(new Intent(this, CusCheckoutActivity.class));
        });
    }
}