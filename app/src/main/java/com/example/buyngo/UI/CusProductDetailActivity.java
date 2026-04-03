package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.buyngo.Model.Product;
import com.example.buyngo.R;
import com.example.buyngo.Store.CartStore;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CusProductDetailActivity extends AppCompatActivity {
    private TextView productName;
    private TextView productCategory;
    private TextView productPrice;
    private TextView productDescription;
    private EditText quantityInput;
    private DatabaseReference db;
    private Product currentProduct;
    private String productId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_product_detail);

        db = FirebaseDatabase.getInstance().getReference();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        productName = findViewById(R.id.productName);
        productCategory = findViewById(R.id.productCategory);
        productPrice = findViewById(R.id.productPrice);
        productDescription = findViewById(R.id.productDescription);
        quantityInput = findViewById(R.id.quantityInput);

        Intent intent = getIntent();
        productId = intent.getStringExtra("productId");

        if (productId == null || productId.isEmpty()) {
            Toast.makeText(this, "Product ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadProductFromFirebase(productId);

        findViewById(R.id.addToCartButton).setOnClickListener(v -> {
            if (currentProduct == null) {
                Toast.makeText(this, "Product data not loaded yet", Toast.LENGTH_SHORT).show();
                return;
            }

            String qtyStr = quantityInput.getText().toString().trim();
            int quantity = qtyStr.isEmpty() ? 1 : Integer.parseInt(qtyStr);

            if (quantity <= 0) {
                Toast.makeText(this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show();
                return;
            }

            CartStore.addToCart(
                    this,
                    currentProduct.getId(),
                    currentProduct.getName(),
                    currentProduct.getCategory(),
                    currentProduct.getPrice(),
                    quantity
            );

            Toast.makeText(this, quantity + " x " + currentProduct.getName() + " added to cart!", Toast.LENGTH_SHORT).show();

            startActivity(new Intent(this, CusCartActivity.class));
        });
    }

    // Fetch product details from Firebase database
    private void loadProductFromFirebase(String productId) {
        db.child("products").child(productId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentProduct = snapshot.getValue(Product.class);

                    if (currentProduct != null) {
                        productName.setText(currentProduct.getName());
                        productCategory.setText(currentProduct.getCategory());
                        productPrice.setText(String.format("$%.2f", currentProduct.getPrice()));
                        productDescription.setText(currentProduct.getDescription());
                    }
                } else {
                    Toast.makeText(CusProductDetailActivity.this, "Product not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(CusProductDetailActivity.this, "Failed to load product: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}