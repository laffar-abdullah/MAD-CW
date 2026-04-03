package com.example.buyngo.UI;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.buyngo.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import androidx.activity.result.ActivityResultLauncher;

import java.util.HashMap;
import java.util.Map;

public class AdmEditProductActivity extends AppCompatActivity {

    private EditText etName, etPrice, etCategory, etDescription, etStock, etBarcode;
    private ProgressBar progressBar;
    private DatabaseReference db;
    private String productId;

    // ZXing barcode scanner launcher
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    etBarcode.setText(result.getContents());
                    Toast.makeText(this, "Barcode scanned: " + result.getContents(),
                            Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adm_edit_product);

        db = FirebaseDatabase.getInstance("https://buyngo-5b43e-default-rtdb.firebaseio.com/").getReference();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Bind views
        etName        = findViewById(R.id.etProductName);
        etPrice       = findViewById(R.id.etProductPrice);
        etCategory    = findViewById(R.id.etProductCategory);
        etDescription = findViewById(R.id.etProductDescription);
        etStock       = findViewById(R.id.etProductStock);
        etBarcode     = findViewById(R.id.etProductBarcode);
        progressBar   = findViewById(R.id.progressBar);

        // Get product ID passed from AdmProductManagementActivity
        productId = getIntent().getStringExtra("productId");
        if (productId == null || productId.isEmpty()) {
            Toast.makeText(this, "Invalid product ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Scan barcode button
        findViewById(R.id.btnScanBarcode).setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan new barcode for product");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            barcodeLauncher.launch(options);
        });

        // Update button
        findViewById(R.id.btnUpdateProductSubmit).setOnClickListener(v -> updateProduct());

        // Load existing product data into the form
        loadProductData();
    }

    /** Fetches the product from Firebase and pre-fills all fields. */
    private void loadProductData() {
        progressBar.setVisibility(View.VISIBLE);

        db.child("products").child(productId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        progressBar.setVisibility(View.GONE);

                        if (!snapshot.exists()) {
                            Toast.makeText(AdmEditProductActivity.this,
                                    "Product not found", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        // Pre-fill each field if the value exists
                        String name        = snapshot.child("name").getValue(String.class);
                        String category    = snapshot.child("category").getValue(String.class);
                        String description = snapshot.child("description").getValue(String.class);
                        String barcode     = snapshot.child("barcode").getValue(String.class);
                        Object price       = snapshot.child("price").getValue();
                        Object stock       = snapshot.child("stock").getValue();

                        if (name        != null) etName.setText(name);
                        if (category    != null) etCategory.setText(category);
                        if (description != null) etDescription.setText(description);
                        if (barcode     != null) etBarcode.setText(barcode);
                        if (price       != null) etPrice.setText(String.valueOf(price));
                        if (stock       != null) etStock.setText(
                                String.valueOf(((Long) stock).intValue()));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AdmEditProductActivity.this,
                                "Failed to load product: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** Validates inputs then writes the updated fields to Firebase. */
    private void updateProduct() {
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

        // Build update map
        Map<String, Object> updates = new HashMap<>();
        updates.put("name",        name);
        updates.put("price",       price);
        updates.put("category",    category);
        updates.put("description", description);
        updates.put("stock",       stock);
        updates.put("barcode",     barcode);

        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.btnUpdateProductSubmit).setEnabled(false);

        db.child("products").child(productId).updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Product updated successfully!",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.btnUpdateProductSubmit).setEnabled(true);
                    Toast.makeText(this, "Update failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}