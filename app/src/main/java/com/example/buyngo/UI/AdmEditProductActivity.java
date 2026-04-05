ckage com.example.buyngo.UI;

import android.content.Intent;
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

public class AdmEditProductActivity extends AppCompatActivity {

    private static final String DB_URL = "https://buyngo-5b43e-default-rtdb.firebaseio.com/";
    private static final int MAX_IMAGE_PX = 600;

    private EditText etName, etPrice, etCategory, etDescription, etStock, etBarcode;
    private ImageView ivProductImage;
    private ProgressBar progressBar;
    private DatabaseReference db;
    private String productId;
    private String existingImageUrl = "";

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

        // Firebase Realtime Database only — no Storage needed
        db = FirebaseDatabase.getInstance(DB_URL).getReference();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

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

        findViewById(R.id.btnPickImage).setOnClickListener(v -> showImagePickerDialog());

        findViewById(R.id.btnScanBarcode).setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan new barcode for product");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            barcodeLauncher.launch(options);
        });

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

                        // Load existing product image — supports both Base64 and plain URLs
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            existingImageUrl = imageUrl;
                            Glide.with(AdmEditProductActivity.this)
                                    .load(imageUrl.startsWith("data:image")
                                            ? decodeBase64ToBitmap(imageUrl)
                                            : imageUrl)
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

        if (selectedImageUri != null) {
            // New image selected — convert to Base64
            String base64Image = uriToBase64(selectedImageUri);
            if (base64Image == null) {
                progressBar.setVisibility(View.GONE);
                findViewById(R.id.btnUpdateProductSubmit).setEnabled(true);
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
                return;
            }
            saveUpdatesToDatabase(name, price, category, description, stock, barcode,
                    "data:image/jpeg;base64," + base64Image);
        } else {
            // Keep the existing image URL (could be Base64 or empty)
            saveUpdatesToDatabase(name, price, category, description,
                    stock, barcode, existingImageUrl);
        }
    }

    
    private String uriToBase64(Uri uri) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            try (InputStream probe = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(probe, null, opts);
            }
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

    
    private Bitmap decodeBase64ToBitmap(String dataUri) {
        try {
            String base64 = dataUri.substring(dataUri.indexOf(",") + 1);
            byte[] bytes = Base64.decode(base64, Base64.NO_WRAP);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
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

