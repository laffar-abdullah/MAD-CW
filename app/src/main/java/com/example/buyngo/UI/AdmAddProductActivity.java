package com.example.buyngo.UI;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.buyngo.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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

public class AdmAddProductActivity extends AppCompatActivity {

    private static final String DB_URL = "https://buyngo-5b43e-default-rtdb.firebaseio.com/";

    private EditText etName, etPrice, etCategory, etDescription, etStock, etBarcode;
    private ImageView ivProductImage;
    private ProgressBar progressBar;
    private DatabaseReference db;
    private StorageReference storageRef;

    private Uri selectedImageUri = null;   // URI of image chosen from gallery or camera
    private Uri cameraImageUri   = null;   // URI of temp file for camera capture

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
        setContentView(R.layout.adm_add_product);

        // Firebase Connection (Thuryas db)
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
        ivProductImage = findViewById(R.id.ivProductImage);

        // Request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 100);
        }

        // Image picker button
        findViewById(R.id.btnPickImage).setOnClickListener(v -> showImagePickerDialog());

        // Barcode scanner button
        findViewById(R.id.btnScanBarcode).setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
            options.setPrompt("Scan product barcode");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            barcodeLauncher.launch(options);
        });

        // Save button
        findViewById(R.id.btnAddProductSubmit).setOnClickListener(v -> saveProduct());
    }

    /** Shows a dialog to choose between Camera and Gallery. */
    private void showImagePickerDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Select Image")
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

    /** Creates a temporary image file for camera capture. */
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        File storageDir = getExternalFilesDir(null);
        return File.createTempFile("IMG_" + timeStamp, ".jpg", storageDir);
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
        findViewById(R.id.btnAddProductSubmit).setEnabled(false);

        // If an image was selected, upload it first then save product
        if (selectedImageUri != null) {
            uploadImageThenSave(name, price, category, description, stock, barcode);
        } else {
            // No image — save directly with empty imageUrl
            saveToDatabase(name, price, category, description, stock, barcode, "");
        }
    }

    /** Uploads image to Firebase Storage then saves product to Realtime Database. */
    private void uploadImageThenSave(String name, double price, String category,
                                     String description, int stock, String barcode) {
        String fileName = "product_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child(fileName);

        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            saveToDatabase(name, price, category, description,
                                    stock, barcode, imageUrl);
                        }))
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.btnAddProductSubmit).setEnabled(true);
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /** Saves the product record to Firebase Realtime Database. */
    private void saveToDatabase(String name, double price, String category,
                                String description, int stock, String barcode,
                                String imageUrl) {
        Map<String, Object> product = new HashMap<>();
        product.put("name",        name);
        product.put("price",       price);
        product.put("category",    category);
        product.put("description", description);
        product.put("stock",       stock);
        product.put("barcode",     barcode);
        product.put("imageUrl",    imageUrl);
        product.put("createdAt",   System.currentTimeMillis());

        db.child("products").push().setValue(product)
                .addOnSuccessListener(unused -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Product added successfully!",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.btnAddProductSubmit).setEnabled(true);
                    Toast.makeText(this, "Failed to save product: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}