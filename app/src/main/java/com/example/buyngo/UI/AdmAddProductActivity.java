package com.example.buyngo.UI;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.buyngo.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.HashMap;
import java.util.Map;

public class AdmAddProductActivity extends AppCompatActivity {

    private EditText etName, etPrice, etCategory, etDescription, etStock, etBarcode;
    private ProgressBar progressBar;
    private DatabaseReference db;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    etBarcode.setText(result.getContents());
                    Toast.makeText(this, "Barcode scanned: " + result.getContents(), Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adm_add_product);

        // Firebase Connection (Thuryas db)
        db = FirebaseDatabase.getInstance("https://buyngo-5b43e-default-rtdb.firebaseio.com/").getReference();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        etName        = findViewById(R.id.etProductName);
        etPrice       = findViewById(R.id.etProductPrice);
        etCategory    = findViewById(R.id.etProductCategory);
        etDescription = findViewById(R.id.etProductDescription);
        etStock       = findViewById(R.id.etProductStock);
        etBarcode     = findViewById(R.id.etProductBarcode);
        progressBar   = findViewById(R.id.progressBar);

        // Request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 100);
        }

        findViewById(R.id.btnScanBarcode).setOnClickListener(v -> launchBarcodeScanner());
        findViewById(R.id.btnAddProductSubmit).setOnClickListener(v -> saveProduct());
    }

    private void launchBarcodeScanner() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
        options.setPrompt("Scan product barcode");
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(false);
        options.setOrientationLocked(true);
        barcodeLauncher.launch(options);
    }

    private void saveProduct() {
        String name        = etName.getText().toString().trim();
        String priceStr    = etPrice.getText().toString().trim();
        String category    = etCategory.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String stockStr    = etStock.getText().toString().trim();
        String barcode     = etBarcode.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(name)) {
            etName.setError("Product name is required");
            etName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(priceStr)) {
            etPrice.setError("Price is required");
            etPrice.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(category)) {
            etCategory.setError("Category is required");
            etCategory.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(stockStr)) {
            etStock.setError("Stock quantity is required");
            etStock.requestFocus();
            return;
        }

        double price;
        int stock;
        try {
            price = Double.parseDouble(priceStr);
            stock = Integer.parseInt(stockStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price or stock value", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build product map
        Map<String, Object> product = new HashMap<>();
        product.put("name",        name);
        product.put("price",       price);
        product.put("category",    category);
        product.put("description", description);
        product.put("stock",       stock);
        product.put("barcode",     barcode);
        product.put("imageUrl",    "");
        product.put("createdAt",   System.currentTimeMillis());

        progressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.btnAddProductSubmit).setEnabled(false);

        // Save to Firebase Realtime Database
        db.child("products").push().setValue(product)
                .addOnSuccessListener(unused -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Product added successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.btnAddProductSubmit).setEnabled(true);
                    Toast.makeText(this, "Failed to add product: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}