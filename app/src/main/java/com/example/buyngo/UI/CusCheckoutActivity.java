package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;

public class CusCheckoutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_checkout);

        // Place Order and go to Tracking
        findViewById(R.id.placeOrderButton).setOnClickListener(v -> {
            startActivity(new Intent(this, CusTrackingActivity.class));
        });
    }
}