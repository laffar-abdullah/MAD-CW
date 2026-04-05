package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
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

public class CusHomeActivity extends AppCompatActivity {
    private LinearLayout productContainer;
    // Firebase connection - reads product data from cloud
    private DatabaseReference db;
    private TextView cartCountBadge;
    private EditText searchBar;
    private List<Product> allProducts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_home);

        // Connect to Firebase database to read products
        db = FirebaseDatabase.getInstance("https://buyngo-5b43e-default-rtdb.firebaseio.com/").getReference();
        productContainer = findViewById(R.id.productContainer);
        searchBar = findViewById(R.id.searchBar);

        ImageButton btnProfile = findViewById(R.id.btnProfile);
        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(this, CusProfileActivity.class)));

        // Add search functionality
        if (searchBar != null) {
            searchBar.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterAndDisplayProducts(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

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
                allProducts.clear();

                if (!dataSnapshot.hasChildren()) {
                    Toast.makeText(CusHomeActivity.this, "No products available", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (DataSnapshot productSnapshot : dataSnapshot.getChildren()) {
                    try {
                        String id = productSnapshot.getKey();
                        String name = productSnapshot.child("name").getValue(String.class);
                        String category = productSnapshot.child("category").getValue(String.class);
                        
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

                        allProducts.add(product);
                    } catch (Exception e) {
                        android.util.Log.e("CusHome", "Error loading product", e);
                        Toast.makeText(CusHomeActivity.this, "Error loading product", Toast.LENGTH_SHORT).show();
                    }
                }

                displayProducts(allProducts);
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

    private void filterAndDisplayProducts(String query) {
        List<Product> filteredProducts = new ArrayList<>();
        String q = query.toLowerCase().trim();

        if (q.isEmpty()) {
            // If search is empty, display all products
            filteredProducts.addAll(allProducts);
        } else {
            // Search by name or category
            for (Product product : allProducts) {
                String name = product.getName() != null ? product.getName().toLowerCase() : "";
                String category = product.getCategory() != null ? product.getCategory().toLowerCase() : "";
                
                if (name.contains(q) || category.contains(q)) {
                    filteredProducts.add(product);
                }
            }
        }

        displayProducts(filteredProducts);
    }

    private void displayProducts(List<Product> products) {
        productContainer.removeAllViews();

        if (products.isEmpty()) {
            TextView emptyMsg = new TextView(CusHomeActivity.this);
            emptyMsg.setText("No products found");
            emptyMsg.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            emptyMsg.setTextSize(16);
            emptyMsg.setPadding(0, 40, 0, 0);
            productContainer.addView(emptyMsg);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(CusHomeActivity.this);
        for (Product product : products) {
            if (product.getName() == null || product.getName().isEmpty()) {
                continue;
            }

            android.view.View cardView = inflater.inflate(R.layout.item_product_card, productContainer, false);

            ImageView productImage = cardView.findViewById(R.id.productImage);
            TextView productName = cardView.findViewById(R.id.productName);
            TextView productCategory = cardView.findViewById(R.id.productCategory);
            TextView productPrice = cardView.findViewById(R.id.productPrice);
            EditText quantityInput = cardView.findViewById(R.id.quantityInput);
            Button addBtn = cardView.findViewById(R.id.addToCartBtn);

            productName.setText(product.getName());
            productCategory.setText(product.getCategory() != null ? product.getCategory() : "N/A");
            productPrice.setText(String.format("Rs. %.2f", product.getPrice()));
            
            String imageUrl = product.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                try {
                    Glide.with(CusHomeActivity.this)
                            .load(imageUrl)
                            .centerCrop()
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .error(android.R.drawable.ic_menu_gallery)
                            .into(productImage);
                } catch (Exception e) {
                    productImage.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } else {
                productImage.setImageResource(android.R.drawable.ic_menu_gallery);
            }
            
            quantityInput.setText("1");

            final Product currentProduct = product;
            
            addBtn.setOnClickListener(v -> {
                String qtyStr = quantityInput.getText().toString().trim();
                int quantity = qtyStr.isEmpty() ? 1 : Integer.parseInt(qtyStr);

                if (quantity <= 0) {
                    Toast.makeText(CusHomeActivity.this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show();
                    return;
                }

                CartStore.addToCart(
                        CusHomeActivity.this,
                        currentProduct.getId(),
                        currentProduct.getName(),
                        currentProduct.getCategory(),
                        currentProduct.getPrice(),
                        quantity
                );

                Toast.makeText(CusHomeActivity.this, quantity + " x " + currentProduct.getName() + " added to cart!", Toast.LENGTH_SHORT).show();
                quantityInput.setText("1");
            });

            productContainer.addView(cardView);
        }
    }
}