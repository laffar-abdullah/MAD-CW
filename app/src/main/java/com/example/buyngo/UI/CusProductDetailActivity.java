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

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 *                   CUSTOMER PRODUCT DETAIL ACTIVITY
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * WHAT THIS SCREEN DOES:\n * Shows detailed information for a selected product (full description, image).\n * Allows customer to select quantity and add to cart.\n * \n * HOW IT CONNECTS TO FIREBASE:\n * 1. CusHomeActivity passes productId to this screen\n * 2. loadProductFromFirebase() reads product details from /products/{productId}/\n * 3. Displays name, category, price, description\n * 4. When "Add to Cart" clicked, saves product to CartStore (local storage)\n * 5. Returns to home/cart\n * \n * DATA FLOW:\n * Firebase /products/{productId}/ → Load into Product object → Display details\n *                                              ↓\n *                                    Customer selects quantity\n *                                              ↓\n *                                    CartStore.addToCart() (local save)\n * \n * IMPORTANT:\n * - Reads product details from Firebase (read-only)\n * - Does NOT save to Firebase directly\n * - Cart stored locally using CartStore\n * ═══════════════════════════════════════════════════════════════════════════════\n */\npublic class CusProductDetailActivity extends AppCompatActivity {
    private TextView productName;
    private TextView productCategory;
    private TextView productPrice;
    private TextView productDescription;
    private EditText quantityInput;
    // Firebase connection - read product details
    private DatabaseReference db;
    private Product currentProduct;
    private String productId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_product_detail);

        // Connect to Firebase to read product data
        db = FirebaseDatabase.getInstance().getReference();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        productName = findViewById(R.id.productName);
        productCategory = findViewById(R.id.productCategory);
        productPrice = findViewById(R.id.productPrice);
        productDescription = findViewById(R.id.productDescription);
        quantityInput = findViewById(R.id.quantityInput);

        // Get product ID passed from CusHomeActivity
        Intent intent = getIntent();
        productId = intent.getStringExtra("productId");

        if (productId == null || productId.isEmpty()) {
            Toast.makeText(this, "Product ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load product details from Firebase
        loadProductFromFirebase(productId);

        // When "Add to Cart" clicked, save to CartStore and return
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

            // Add to CartStore (local phone storage)
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

    /**
     * Load product details from Firebase database
     */
    private void loadProductFromFirebase(String productId) {
        // STEP 1: Connect to Firebase database and read specific product by ID
        db.child("products").child(productId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // STEP 2: Check if product exists in Firebase
                if (snapshot.exists()) {
                    try {
                        // STEP 3: Create new Product object to store data
                        currentProduct = new Product();
                        // STEP 4: Get product ID from Firebase
                        currentProduct.setId(snapshot.getKey());
                        // STEP 5: Get product name from Firebase
                        currentProduct.setName(snapshot.child("name").getValue(String.class));
                        // STEP 6: Get product category from Firebase
                        currentProduct.setCategory(snapshot.child("category").getValue(String.class));
                        // STEP 7: Get product description from Firebase
                        currentProduct.setDescription(snapshot.child("description").getValue(String.class));
                        // STEP 8: Get product image URL from Firebase
                        currentProduct.setImageUrl(snapshot.child("imageUrl").getValue(String.class));
                        
                        // STEP 9: Get price from Firebase (handle different data types: Double, Long, or String)
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
                        // STEP 10: Set price on Product object
                        currentProduct.setPrice(price);
                        
                        // STEP 11: Get available stock from Firebase
                        Long stock = snapshot.child("stock").getValue(Long.class);
                        // STEP 12: Set stock on Product object
                        currentProduct.setStock(stock != null ? stock.intValue() : 0);

                        // STEP 13: Set product name on screen
                        productName.setText(currentProduct.getName());
                        // STEP 14: Set product category on screen
                        productCategory.setText(currentProduct.getCategory());
                        // STEP 15: Set product price on screen
                        productPrice.setText(String.format("Rs. %.2f", currentProduct.getPrice()));
                        // STEP 16: Set product description on screen
                        productDescription.setText(currentProduct.getDescription());
                    } catch (Exception e) {
                        // STEP 17: If error loading product, show error message
                        Toast.makeText(CusProductDetailActivity.this, "Error loading product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        // STEP 18: Close screen if error
                        finish();
                    }
                } else {
                    // STEP 19: If product doesn't exist in Firebase, show error
                    Toast.makeText(CusProductDetailActivity.this, "Product not found", Toast.LENGTH_SHORT).show();
                    // STEP 20: Close screen
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