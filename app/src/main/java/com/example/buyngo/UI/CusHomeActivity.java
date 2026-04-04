package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.Model.Product;
import com.example.buyngo.R;
import com.example.buyngo.Store.CartStore;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════════\n *                         CUSTOMER HOME ACTIVITY\n * ═══════════════════════════════════════════════════════════════════════════════\n * \n * WHAT THIS SCREEN DOES:\n * This is the main shopping screen. Shows all available products from Firebase.\n * Customers can browse items, select quantity, and add to cart.\n * \n * HOW IT CONNECTS TO FIREBASE:\n * 1. When screen opens, loadProductsFromFirebase() is called\n * 2. Reads /products/ collection from Firebase database\n * 3. Each product is converted to a Product model object\n * 4. Displays products as cards on screen\n * 5. When customer adds to cart, product data saved to CartStore (local storage)\n * \n * DATA FLOW:\n * Firebase /products/ → Load into Product objects → Display as cards\n *                                     ↓\n *                           Customer taps \"Add to Cart\"\n *                                     ↓\n *                  Product data saved to CartStore (local phone storage)\n * \n * IMPORTANT NOTES:\n * - This screen READS products from Firebase (read-only)\n * - Cart is stored locally (not in Firebase) using CartStore\n * - Bottom navigation allows jumping to cart, orders, feedback, profile\n * ═══════════════════════════════════════════════════════════════════════════════\n */\npublic class CusHomeActivity extends AppCompatActivity {
    private LinearLayout productContainer;
    // Firebase connection - reads product data from cloud
    private DatabaseReference db;
    private TextView cartCountBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_home);

        // Connect to Firebase database to read products
        db = FirebaseDatabase.getInstance("https://buyngo-5b43e-default-rtdb.firebaseio.com/").getReference();
        productContainer = findViewById(R.id.productContainer);

        ImageButton btnProfile = findViewById(R.id.btnProfile);
        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(this, CusProfileActivity.class)));

        // Load all products from Firebase and display them
        loadProductsFromFirebase();

        // Set up bottom navigation menu
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // Already on home, do nothing
                return true;
            } else if (id == R.id.nav_cart) {
                startActivity(new Intent(this, CusCartActivity.class));
            } else if (id == R.id.nav_orders) {
                startActivity(new Intent(this, CusTrackingActivity.class));
            } else if (id == R.id.nav_reviews) {
                startActivity(new Intent(this, CusFeedbackActivity.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, CusProfileActivity.class));
            }
            return true;
        });
    }

    // Load all products from Firebase database
    private void loadProductsFromFirebase() {
        db.child("products").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                productContainer.removeAllViews();

                if (!dataSnapshot.hasChildren()) {
                    Toast.makeText(CusHomeActivity.this, "No products available", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<Product> products = new ArrayList<>();
                for (DataSnapshot productSnapshot : dataSnapshot.getChildren()) {
                    try {
                        String id = productSnapshot.getKey();
                        String name = productSnapshot.child("name").getValue(String.class);
                        String category = productSnapshot.child("category").getValue(String.class);
                        
                        // Handle price which might be Double, Long, or String in Firebase
                        Double price = 0.0;
                        Object priceObj = productSnapshot.child("price").getValue();
                        if (priceObj != null) {
                            if (priceObj instanceof Double) {
                                price = (Double) priceObj;
                            } else if (priceObj instanceof Long) {
                                price = ((Long) priceObj).doubleValue();
                            } else if (priceObj instanceof String) {
                                try {
                                    price = Double.parseDouble((String) priceObj);
                                } catch (NumberFormatException e) {
                                    android.util.Log.w("CusHome", "Invalid price format: " + priceObj);
                                    price = 0.0;
                                }
                            }
                        }
                        
                        String description = productSnapshot.child("description").getValue(String.class);
                        String imageUrl = productSnapshot.child("imageUrl").getValue(String.class);
                        Long stock = productSnapshot.child("stock").getValue(Long.class);

                        Product product = new Product();
                        product.setId(id);
                        product.setName(name);
                        product.setCategory(category);
                        product.setPrice(price);
                        product.setDescription(description);
                        product.setImageUrl(imageUrl);
                        product.setStock(stock != null ? stock.intValue() : 0);

                        products.add(product);
                    } catch (Exception e) {
                        Toast.makeText(CusHomeActivity.this, "Error loading product", Toast.LENGTH_SHORT).show();
                    }
                }

                LayoutInflater inflater = LayoutInflater.from(CusHomeActivity.this);
                for (Product product : products) {
                    if (product.getName() == null || product.getName().isEmpty()) {
                        continue;
                    }

                    android.view.View cardView = inflater.inflate(R.layout.item_product_card, productContainer, false);

                    TextView productName = cardView.findViewById(R.id.productName);
                    TextView productCategory = cardView.findViewById(R.id.productCategory);
                    TextView productPrice = cardView.findViewById(R.id.productPrice);
                    EditText quantityInput = cardView.findViewById(R.id.quantityInput);
                    Button addBtn = cardView.findViewById(R.id.addToCartBtn);

                    productName.setText(product.getName());
                    productCategory.setText(product.getCategory() != null ? product.getCategory() : "N/A");
                    productPrice.setText(String.format("Rs. %.2f", product.getPrice()));
                    
                    // Set default quantity to 1
                    quantityInput.setText("1");

                    // Create final copy to avoid closure issue with loop variable
                    final Product currentProduct = product;
                    
                    addBtn.setOnClickListener(v -> {
                        String qtyStr = quantityInput.getText().toString().trim();
                        int quantity = qtyStr.isEmpty() ? 1 : Integer.parseInt(qtyStr);

                        if (quantity <= 0) {
                            Toast.makeText(CusHomeActivity.this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Add directly to cart without going to product detail page
                        CartStore.addToCart(
                                CusHomeActivity.this,
                                currentProduct.getId(),
                                currentProduct.getName(),
                                currentProduct.getCategory(),
                                currentProduct.getPrice(),
                                quantity
                        );

                        Toast.makeText(CusHomeActivity.this, quantity + " x " + currentProduct.getName() + " added to cart!", Toast.LENGTH_SHORT).show();
                        quantityInput.setText("1"); // Reset quantity for next purchase
                    });

                    productContainer.addView(cardView);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(CusHomeActivity.this, "Failed to load products", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProductsFromFirebase();
        updateCartCounter();
    }

    /**
     * Updates the cart item counter on the home screen
     */
    private void updateCartCounter() {
        int cartCount = CartStore.getCartItemCount(this);
        if (cartCountBadge != null) {
            if (cartCount > 0) {
                cartCountBadge.setText(String.valueOf(cartCount));
                cartCountBadge.setVisibility(android.view.View.VISIBLE);
            } else {
                cartCountBadge.setVisibility(android.view.View.GONE);
            }
        }
    }
}