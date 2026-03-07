package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;

public class CusProductDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_product_detail);

        // Add to Cart and go to Cart screen
        findViewById(R.id.addToCartButton).setOnClickListener(v -> {
            startActivity(new Intent(this, CusCartActivity.class));
        });
    }
}