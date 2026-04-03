package com.example.buyngo.UI;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.buyngo.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.List;

public class AdmProductManagementActivity extends AppCompatActivity {

    private DatabaseReference db;
    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private EditText etSearch;

    private final List<DataSnapshot> allProducts      = new ArrayList<>();
    private final List<DataSnapshot> filteredProducts = new ArrayList<>();

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    etSearch.setText(result.getContents());
                    filterByBarcode(result.getContents());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adm_product_management);

        db = FirebaseDatabase.getInstance().getReference();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBar);
        tvEmpty     = findViewById(R.id.tvEmpty);
        etSearch    = findViewById(R.id.etSearch);

        recyclerView = findViewById(R.id.recyclerProducts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter(filteredProducts);
        recyclerView.setAdapter(adapter);

        // Request camera permission on launch
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 100);
        }

        findViewById(R.id.btnAddProduct).setOnClickListener(v ->
                startActivity(new Intent(this, AdmAddProductActivity.class)));

        findViewById(R.id.btnScanBarcode).setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan barcode to find product");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true); 
            barcodeLauncher.launch(options);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { filterProducts(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadProducts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProducts();
    }

    private void loadProducts() {
        progressBar.setVisibility(View.VISIBLE);
        db.child("products").orderByChild("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        progressBar.setVisibility(View.GONE);
                        allProducts.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            allProducts.add(child);
                        }
                        filterProducts(etSearch.getText().toString());
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AdmProductManagementActivity.this, "Failed to load products", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filterProducts(String query) {
        filteredProducts.clear();
        String q = query.toLowerCase().trim();
        for (DataSnapshot snap : allProducts) {
            String name     = snap.child("name").getValue(String.class);
            String category = snap.child("category").getValue(String.class);
            if (q.isEmpty()
                    || (name != null && name.toLowerCase().contains(q))
                    || (category != null && category.toLowerCase().contains(q))) {
                filteredProducts.add(snap);
            }
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(filteredProducts.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void filterByBarcode(String barcode) {
        filteredProducts.clear();
        for (DataSnapshot snap : allProducts) {
            String bc = snap.child("barcode").getValue(String.class);
            if (barcode.equals(bc)) {
                filteredProducts.add(snap);
            }
        }
        if (filteredProducts.isEmpty())
            Toast.makeText(this, "No product found for barcode: " + barcode, Toast.LENGTH_SHORT).show();
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(filteredProducts.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void confirmDelete(DataSnapshot snap) {
        String name = snap.child("name").getValue(String.class);
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete \"" + name + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.child("products").child(snap.getKey()).removeValue()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Product deleted", Toast.LENGTH_SHORT).show();
                                loadProducts();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inner RecyclerView Adapter
    // ─────────────────────────────────────────────────────────────────────────

    private class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductVH> {

        private final List<DataSnapshot> items;

        ProductAdapter(List<DataSnapshot> items) { this.items = items; }

        @NonNull
        @Override
        public ProductVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_product_admin, parent, false);
            return new ProductVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ProductVH h, int pos) {
            DataSnapshot snap = items.get(pos);

            String name     = snap.child("name").getValue(String.class);
            String category = snap.child("category").getValue(String.class);
            Object priceObj = snap.child("price").getValue();
            Object stockObj = snap.child("stock").getValue();

            h.tvName.setText(name != null ? name : "Unnamed");
            h.tvCategory.setText(category != null ? category : "—");
            h.tvPrice.setText(priceObj != null ? "LKR " + priceObj : "—");
            h.tvStock.setText(stockObj != null ? "Stock: " + stockObj : "Stock: —");

            h.itemView.findViewById(R.id.btnEdit).setOnClickListener(v -> {
                Intent intent = new Intent(AdmProductManagementActivity.this, AdmEditProductActivity.class);
                intent.putExtra("productId", snap.getKey());
                startActivity(intent);
            });

            h.itemView.findViewById(R.id.btnDelete).setOnClickListener(v -> confirmDelete(snap));
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ProductVH extends RecyclerView.ViewHolder {
            TextView tvName, tvCategory, tvPrice, tvStock;

            ProductVH(@NonNull View itemView) {
                super(itemView);
                tvName     = itemView.findViewById(R.id.tvProductName);
                tvCategory = itemView.findViewById(R.id.tvProductCategory);
                tvPrice    = itemView.findViewById(R.id.tvProductPrice);
                tvStock    = itemView.findViewById(R.id.tvProductStock);
            }
        }
    }
}