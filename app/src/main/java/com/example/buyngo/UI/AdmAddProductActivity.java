package com.example.buyngo.UI;

import android.os.Bundle;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.buyngo.R;

public class AdmAddProductActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adm_add_product);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        EditText etName = findViewById(R.id.etProductName);
        EditText etPrice = findViewById(R.id.etProductPrice);
        EditText etCategory = findViewById(R.id.etProductCategory);

        findViewById(R.id.btnAddProductSubmit).setOnClickListener(v -> {
            String name = etName.getText().toString();
            String price = etPrice.getText().toString();
            String category = etCategory.getText().toString();
            
            // Logic to add product would go here
            finish();
        });
    }
}