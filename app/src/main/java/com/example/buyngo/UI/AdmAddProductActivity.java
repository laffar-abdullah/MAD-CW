package com.example.buyngo.UI;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
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
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdmAddProductActivity extends AppCompatActivity {

    private static final String DB_URL = "https://buyngo-5b43e-default-rtdb.firebaseio.com/";
    // Max image dimension (px) — keeps Base64 size reasonable in the DB
    private static final int MAX_IMAGE_PX = 600;

    private EditText etName, etPrice, etCategory, etDescription, etStock, etBarcode;
    private ImageView ivProductImage;
    private ProgressBar progressBar;
    private DatabaseReference db;

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
        setContentView(R.layout.adm_add_product);

        // Firebase Realtime Database only — no Storage needed
        db = FirebaseDatabase.getInstance(DB_URL).getReference();

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
        ivProductImage = findViewById(R.id.ivProductImage);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 100);
        }

        findViewById(R.id.btnPickImage).setOnClickListener(v -> showImagePickerDialog());

        findViewById(R.id.btnScanBarcode).setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
            options.setPrompt("Scan product barcode");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            barcodeLauncher.launch(options);
        });

        findViewById(R.id.btnAddProductSubmit).setOnClickListener(v -> saveProduct());
    }

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

        if (selectedImageUri != null) {
            String base64Image = uriToBase64(selectedImageUri);
            if (base64Image == null) {
                progressBar.setVisibility(View.GONE);
                findViewById(R.id.btnAddProductSubmit).setEnabled(true);
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
                return;
            }
            saveToDatabase(name, price, category, description, stock, barcode,
                    "data:image/jpeg;base64," + base64Image);
        } else {
            saveToDatabase(name, price, category, description, stock, barcode, "");
        }
    }

    
    private String uriToBase64(Uri uri) {
        try {
            // First pass: measure dimensions only
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            try (InputStream probe = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(probe, null, opts);
            }
            // Calculate sub-sampling factor
            int sampleSize = 1;
            int w = opts.outWidth, h = opts.outHeight;
            while (w / sampleSize > MAX_IMAGE_PX || h / sampleSize > MAX_IMAGE_PX) {
                sampleSize *= 2;
            }
            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sampleSize;

            Bitmap bitmap;
            try (InputStream stream = getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(stream, null, opts);
            }
            if (bitmap == null) return null;

            // Fine-scale if still larger than MAX_IMAGE_PX
            int maxDim = Math.max(bitmap.getWidth(), bitmap.getHeight());
            if (maxDim > MAX_IMAGE_PX) {
                float scale = (float) MAX_IMAGE_PX / maxDim;
                int newW = Math.round(bitmap.getWidth()  * scale);
                int newH = Math.round(bitmap.getHeight() * scale);
                bitmap = Bitmap.createScaledBitmap(bitmap, newW, newH, true);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos);
            return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    
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

