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
        toolbar.setNavigationOnClickListener(v -> finish());

        // Edit buttons -> open Edit Product screen
        findViewById(R.id.editBtn1).setOnClickListener(v ->
                startActivity(new Intent(this, AdmEditProductActivity.class)));
        findViewById(R.id.editBtn2).setOnClickListener(v ->
                startActivity(new Intent(this, AdmEditProductActivity.class)));
        findViewById(R.id.editBtn3).setOnClickListener(v ->
                startActivity(new Intent(this, AdmEditProductActivity.class)));

        // FAB -> Add Product screen
        findViewById(R.id.fabAddProduct).setOnClickListener(v ->
                startActivity(new Intent(this, AdmAddProductActivity.class)));
    }
}