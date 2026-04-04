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
            quantityInput.setText(""); // Clear the quantity input for next product
            finish();
        });
    }

    // Fetch product details from Firebase database
    private void loadProductFromFirebase(String productId) {
        db.child("products").child(productId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        currentProduct = new Product();
                        currentProduct.setId(snapshot.getKey());
                        currentProduct.setName(snapshot.child("name").getValue(String.class));
                        currentProduct.setCategory(snapshot.child("category").getValue(String.class));
                        currentProduct.setDescription(snapshot.child("description").getValue(String.class));
                        currentProduct.setImageUrl(snapshot.child("imageUrl").getValue(String.class));
                        
                        // Handle price which might be Double, Long, or String in Firebase
                        Double price = 0.0;
                        Object priceObj = snapshot.child("price").getValue();
                        if (priceObj != null) {
                            if (priceObj instanceof Double) {
                                price = (Double) priceObj;
                            } else if (priceObj instanceof Long) {
                                price = ((Long) priceObj).doubleValue();
                            } else if (priceObj instanceof String) {
                                try {
                                    price = Double.parseDouble((String) priceObj);
                                } catch (NumberFormatException e) {
                                    android.util.Log.w("ProductDetail", "Invalid price format: " + priceObj);
                                    price = 0.0;
                                }
                            }
                        }
                        currentProduct.setPrice(price);
                        
                        Long stock = snapshot.child("stock").getValue(Long.class);
                        currentProduct.setStock(stock != null ? stock.intValue() : 0);

                        productName.setText(currentProduct.getName());
                        productCategory.setText(currentProduct.getCategory());
                        productPrice.setText(String.format("Rs. %.2f", currentProduct.getPrice()));
                        productDescription.setText(currentProduct.getDescription());
                    } catch (Exception e) {
                        Toast.makeText(CusProductDetailActivity.this, "Error loading product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
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