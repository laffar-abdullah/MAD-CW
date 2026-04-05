ackage com.example.buyngo.UI;

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

