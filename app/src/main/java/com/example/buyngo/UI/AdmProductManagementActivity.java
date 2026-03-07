package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.buyngo.R;

public class AdmProductManagementActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adm_product_management);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.btnAddProduct).setOnClickListener(v -> 
            startActivity(new Intent(this, AdmAddProductActivity.class)));

        findViewById(R.id.btnEditProduct).setOnClickListener(v -> 
            startActivity(new Intent(this, AdmEditProductActivity.class)));

        findViewById(R.id.btnDeleteProduct).setOnClickListener(v -> {
            // Logic for delete product
        });
    }
}