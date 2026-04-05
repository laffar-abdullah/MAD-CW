ackage com.example.buyngo.UI;

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

public class CusHomeActivity extends AppCompatActivity {
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
        // STEP 1: Connect to Firebase database /products/ collection
        db.child("products").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // STEP 2: Clear screen (remove old products before showing new ones)
                productContainer.removeAllViews();

                // STEP 3: Check if any products exist in database
                if (!dataSnapshot.hasChildren()) {
                    Toast.makeText(CusHomeActivity.this, "No products available", Toast.LENGTH_SHORT).show();
                    return;
                }

                // STEP 4: Create empty list to store all products
                List<Product> products = new ArrayList<>();
                
                // STEP 5: Loop through each product in Firebase database
                for (DataSnapshot productSnapshot : dataSnapshot.getChildren()) {
                    try {
                        // STEP 6: Get product ID from Firebase
                        String id = productSnapshot.getKey();
                        // STEP 7: Get product name from Firebase
                        String name = productSnapshot.child("name").getValue(String.class);
                        // STEP 8: Get product category from Firebase
                        String category = productSnapshot.child("category").getValue(String.class);
                        
                        // STEP 9: Get price from Firebase (handle different data types: Double, Long, String)
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
                        
                        // STEP 10: Get product description from Firebase
                        String description = productSnapshot.child("description").getValue(String.class);
                        // STEP 11: Get product image URL from Firebase
                        String imageUrl = productSnapshot.child("imageUrl").getValue(String.class);
                        // STEP 12: Get available stock from Firebase
                        Long stock = productSnapshot.child("stock").getValue(Long.class);

                        // STEP 13: Create new Product object and set all fields from Firebase data
                        Product product = new Product();
                        product.setId(id);
                        product.setName(name);
                        product.setCategory(category);
                        product.setPrice(price);
                        product.setDescription(description);
                        product.setImageUrl(imageUrl);
                        product.setStock(stock != null ? stock.intValue() : 0);

                        // STEP 14: Add product to list (now in memory, not in Firebase)
                        products.add(product);
                    } catch (Exception e) {
                        Toast.makeText(CusHomeActivity.this, "Error loading product", Toast.LENGTH_SHORT).show();
                    }
                }

                // STEP 15: Create layout inflater to make product cards (UI elements)
                LayoutInflater inflater = LayoutInflater.from(CusHomeActivity.this);
                
                // STEP 16: Loop through products again and create visual card for each one
                for (Product product : products) {
                    // STEP 17: Skip products with no name
                    if (product.getName() == null || product.getName().isEmpty()) {
                        continue;
                    }

                    // STEP 18: Inflate product card layout from XML file
                    android.view.View cardView = inflater.inflate(R.layout.item_product_card, productContainer, false);

                    // STEP 19: Get UI elements from card (name, category, price, quantity, button)
                    TextView productName = cardView.findViewById(R.id.productName);
                    TextView productCategory = cardView.findViewById(R.id.productCategory);
                    TextView productPrice = cardView.findViewById(R.id.productPrice);
                    EditText quantityInput = cardView.findViewById(R.id.quantityInput);
                    Button addBtn = cardView.findViewById(R.id.addToCartBtn);

                    // STEP 20: Set text on card (product name)
                    productName.setText(product.getName());
                    // STEP 21: Set text on card (product category)
                    productCategory.setText(product.getCategory() != null ? product.getCategory() : "N/A");
                    // STEP 22: Set text on card (product price)
                    productPrice.setText(String.format("Rs. %.2f", product.getPrice()));
                    
                    // STEP 23: Set default quantity to 1
                    quantityInput.setText("1");

                    // STEP 24: Create final copy to avoid closure issue with loop variable
                    final Product currentProduct = product;
                    
                    // STEP 25: When customer clicks "Add to Cart" button
                    addBtn.setOnClickListener(v -> {
                        // STEP 26: Get quantity customer entered
                        String qtyStr = quantityInput.getText().toString().trim();
                        // STEP 27: Convert quantity string to number (default 1 if empty)
                        int quantity = qtyStr.isEmpty() ? 1 : Integer.parseInt(qtyStr);

                        // STEP 28: Validate quantity is greater than 0
                        if (quantity <= 0) {
                            Toast.makeText(CusHomeActivity.this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // STEP 29: Call CartStore to add product to local phone storage (NOT Firebase)
                        CartStore.addToCart(
                                CusHomeActivity.this,
                                currentProduct.getId(),
                                currentProduct.getName(),
                                currentProduct.getCategory(),
                                currentProduct.getPrice(),
                                quantity
                        );

                        // STEP 30: Show confirmation message to customer
                        Toast.makeText(CusHomeActivity.this, quantity + " x " + currentProduct.getName() + " added to cart!", Toast.LENGTH_SHORT).show();
                        // STEP 31: Reset quantity field back to 1 for next product
                        quantityInput.setText("1");
                    });

                    // STEP 32: Add card to screen
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

