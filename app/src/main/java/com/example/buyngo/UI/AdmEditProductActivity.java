package com.example.buyngo.UI;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.buyngo.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdmEditProductActivity extends AppCompatActivity {

    private static final String DB_URL = "https://buyngo-5b43e-default-rtdb.firebaseio.com/";

    private EditText etName, etPrice, etCategory, etDescription, etStock, etBarcode;
    private ImageView ivProductImage;
    private ProgressBar progressBar;
    private DatabaseReference db;
    private StorageReference storageRef;
    private String productId;
    private String existingImageUrl = ""; // keeps current image if user doesn't change it

    private Uri selectedImageUri = null;
    private Uri cameraImageUri   = null;

    // ── Barcode scanner ──────────────────────────────────────────────────────
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    etBarcode.setText(result.getContents());
                    Toast.makeText(this, "Barcode: " + result.getContents(),
                            Toast.LENGTH_SHORT).show();
                }
            });

    // ── Gallery picker ───────────────────────────────────────────────────────
    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this).load(selectedImageUri).centerCrop().into(ivProductImage);
                }
            });

    // ── Camera capture ───────────────────────────────────────────────────────
    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraImageUri != null) {
                    selectedImageUri = cameraImageUri;
                    Glide.with(this).load(selectedImageUri).centerCrop().into(ivProductImage);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adm_edit_product);

        db         = FirebaseDatabase.getInstance(DB_URL).getReference();
        storageRef = FirebaseStorage.getInstance().getReference("product_images");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Bind views
        etName         = findViewById(R.id.etProductName);
        etPrice        = findViewById(R.id.etProductPrice);
        etCategory     = findViewById(R.id.etProductCategory);
        etDescription  = findViewById(R.id.etProductDescription);
        etStock        = findViewById(R.id.etProductStock);
        etBarcode      = findViewById(R.id.etProductBarcode);
        progressBar    = findViewById(R.id.progressBar);
        ivProductImage = findViewById(R.id.ivProductImage);

        productId = getIntent().getStringExtra("productId");
        if (productId == null || productId.isEmpty()) {
            Toast.makeText(this, "Invalid product ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Image picker button
        findViewById(R.id.btnPickImage).setOnClickListener(v -> showImagePickerDialog());

        // Barcode scanner button
        findViewById(R.id.btnScanBarcode).setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan new barcode for product");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            barcodeLauncher.launch(options);
        });

        // Update button
        findViewById(R.id.btnUpdateProductSubmit).setOnClickListener(v -> updateProduct());

        loadProductData();
    }

    private void showImagePickerDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Change Image")
                .setItems(new String[]{"Take Photo", "Choose from Gallery"}, (dialog, which) -> {
                    if (which == 0) openCamera();
                    else openGallery();
                })
                .show();
    }

    private void openCamera() {
        try {
            File photoFile = createImageFile();
            cameraImageUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", photoFile);
            cameraLauncher.launch(cameraImageUri);
        } catch (IOException e) {
            Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        File storageDir = getExternalFilesDir(null);
        return File.createTempFile("IMG_" + timeStamp, ".jpg", storageDir);
    }

    /** Loads existing product data from Firebase and fills the form. */
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

                        String name        = snapshot.child("name").getValue(String.class);
                        String category    = snapshot.child("category").getValue(String.class);
                        String description = snapshot.child("description").getValue(String.class);
                        String barcode     = snapshot.child("barcode").getValue(String.class);
                        String imageUrl    = snapshot.child("imageUrl").getValue(String.class);
                        Object price       = snapshot.child("price").getValue();
                        Object stock       = snapshot.child("stock").getValue();

                        if (name        != null) etName.setText(name);
                        if (category    != null) etCategory.setText(category);
                        if (description != null) etDescription.setText(description);
                        if (barcode     != null) etBarcode.setText(barcode);
                        if (price       != null) etPrice.setText(String.valueOf(price));
                        if (stock       != null) etStock.setText(
                                String.valueOf(((Long) stock).intValue()));

                        // Load existing product image
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            existingImageUrl = imageUrl;
                            Glide.with(AdmEditProductActivity.this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.ic_launcher_foreground)
                                    .centerCrop()
                                    .into(ivProductImage);
                        }
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

    private void updateProduct() {
        String name        = etName.getText().toString().trim();
        String priceStr    = etPrice.getText().toString().trim();
        String category    = etCategory.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String stockStr    = etStock.getText().toString().trim();
        String barcode     = etBarcode.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Product name is required");
            etName.requestFocus(); return;
        }
        if (TextUtils.isEmpty(priceStr)) {
            etPrice.setError("Price is required");
            etPrice.requestFocus(); return;
        }
        if (TextUtils.isEmpty(category)) {
            etCategory.setError("Category is required");
            etCategory.requestFocus(); return;
        }
        if (TextUtils.isEmpty(stockStr)) {
            etStock.setError("Stock quantity is required");
            etStock.requestFocus(); return;
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

        progressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.btnUpdateProductSubmit).setEnabled(false);

        // If a new image was selected, upload it first
        if (selectedImageUri != null) {
            uploadImageThenUpdate(name, price, category, description, stock, barcode);
        } else {
            // Keep the existing image URL
            saveUpdatesToDatabase(name, price, category, description,
                    stock, barcode, existingImageUrl);
        }
    }

    private void uploadImageThenUpdate(String name, double price, String category,
                                       String description, int stock, String barcode) {
        String fileName = "product_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child(fileName);

        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        imageRef.getDownloadUrl().addOnSuccessListener(uri ->
                                saveUpdatesToDatabase(name, price, category, description,
                                        stock, barcode, uri.toString())))
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.btnUpdateProductSubmit).setEnabled(true);
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void saveUpdatesToDatabase(String name, double price, String category,
                                       String description, int stock, String barcode,
                                       String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name",        name);
        updates.put("price",       price);
        updates.put("category",    category);
        updates.put("description", description);
        updates.put("stock",       stock);
        updates.put("barcode",     barcode);
        updates.put("imageUrl",    imageUrl);

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