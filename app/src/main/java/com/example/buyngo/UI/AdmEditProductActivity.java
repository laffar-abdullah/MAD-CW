package com.example.buyngo.UI;

import android.os.Bundle;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.buyngo.R;

public class AdmEditProductActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adm_edit_product);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        EditText etName = findViewById(R.id.etProductName);
        EditText etPrice = findViewById(R.id.etProductPrice);
        EditText etCategory = findViewById(R.id.etProductCategory);

        findViewById(R.id.btnUpdateProductSubmit).setOnClickListener(v -> {
            String name = etName.getText().toString();
            String price = etPrice.getText().toString();
            String category = etCategory.getText().toString();
            
            // Logic to update product would go here
            finish();
        });
    }
}