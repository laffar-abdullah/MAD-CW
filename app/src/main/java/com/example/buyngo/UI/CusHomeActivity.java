package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.Model.Product;
import com.example.buyngo.R;
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
    private DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_home);

        // Connect to Firebase database
        db = FirebaseDatabase.getInstance("https://buyngo-5b43e-default-rtdb.firebaseio.com/").getReference();
        productContainer = findViewById(R.id.productContainer);

        ImageButton btnProfile = findViewById(R.id.btnProfile);
        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(this, CusProfileActivity.class)));

        loadProductsFromFirebase();

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
                        Double price = productSnapshot.child("price").getValue(Double.class);
                        String description = productSnapshot.child("description").getValue(String.class);
                        String imageUrl = productSnapshot.child("imageUrl").getValue(String.class);
                        Long stock = productSnapshot.child("stock").getValue(Long.class);

                        Product product = new Product();
                        product.setId(id);
                        product.setName(name);
                        product.setCategory(category);
                        product.setPrice(price != null ? price : 0.0);
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
                    Button addBtn = cardView.findViewById(R.id.addToCartBtn);

                    productName.setText(product.getName());
                    productCategory.setText(product.getCategory() != null ? product.getCategory() : "N/A");
                    productPrice.setText(String.format("$%.2f", product.getPrice()));

                    // Create final copy to avoid closure issue with loop variable
                    final Product currentProduct = product;
                    addBtn.setOnClickListener(v -> {
                        Intent intent = new Intent(CusHomeActivity.this, CusProductDetailActivity.class);
                        intent.putExtra("productId", currentProduct.getId());
                        startActivity(intent);
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
    }
}