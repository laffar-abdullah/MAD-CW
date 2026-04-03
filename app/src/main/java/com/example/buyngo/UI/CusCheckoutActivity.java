package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.buyngo.Model.Order;
import com.example.buyngo.Model.User;
import com.example.buyngo.R;
import com.example.buyngo.Store.CartStore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Checkout screen - displays order summary and delivery address
public class CusCheckoutActivity extends AppCompatActivity {
    private TextView totalPriceText;
    private EditText phoneEditText;
    private EditText addressEditText;
    private EditText cityEditText;
    private Button confirmButton;
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_checkout);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        totalPriceText = findViewById(R.id.totalPriceText);
        phoneEditText = findViewById(R.id.phoneEditText);
        addressEditText = findViewById(R.id.addressEditText);
        cityEditText = findViewById(R.id.cityEditText);
        confirmButton = findViewById(R.id.confirmButton);

        // Load customer's address from Firebase
        loadCustomerAddress();

        // Calculate and display total
        double total = CartStore.getCartTotal(this);
        totalPriceText.setText(String.format("$%.2f", total));

        confirmButton.setOnClickListener(v -> placeOrder());
    }

    // Load address details from user profile
    private void loadCustomerAddress() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = firebaseAuth.getCurrentUser().getUid();

        firebaseDatabase.getReference("users").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            User user = snapshot.getValue(User.class);
                            if (user != null) {
                                phoneEditText.setText(user.getPhoneNumber());
                                addressEditText.setText(user.getAddress());
                                cityEditText.setText(user.getCity());
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(CusCheckoutActivity.this,
                                "Failed to load address: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Place order - save to Firebase and clear cart
    private void placeOrder() {
        String phone = phoneEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String city = cityEditText.getText().toString().trim();

        if (phone.isEmpty() || address.isEmpty() || city.isEmpty()) {
            Toast.makeText(this, "Please fill all address fields", Toast.LENGTH_SHORT).show();
            return;
        }

        confirmButton.setEnabled(false);
        confirmButton.setText("Placing Order...");

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            confirmButton.setEnabled(true);
            confirmButton.setText("Confirm Order");
            return;
        }

        String userId = firebaseAuth.getCurrentUser().getUid();

        // Get all cart items
        List<CartStore.CartItem> cartItems = CartStore.getCartItems(this);
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
            confirmButton.setEnabled(true);
            confirmButton.setText("Confirm Order");
            return;
        }

        // Get customer name from Firebase
        firebaseDatabase.getReference("users").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            User user = snapshot.getValue(User.class);
                            if (user != null) {
                                createAndSaveOrder(userId, user.getFullName(), cartItems);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(CusCheckoutActivity.this,
                                "Failed to get customer info: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        confirmButton.setEnabled(true);
                        confirmButton.setText("Confirm Order");
                    }
                });
    }

    // Create order object and save to Firebase
    private void createAndSaveOrder(String customerId, String customerName, List<CartStore.CartItem> cartItems) {
        // Generate order ID
        String orderId = "ORD-" + System.currentTimeMillis();

        // Create items map
        Map<String, Integer> items = new HashMap<>();
        for (CartStore.CartItem item : cartItems) {
            items.put(item.name, item.quantity);
        }

        // Calculate total
        double totalAmount = CartStore.getCartTotal(this);

        // Debug logging
        android.util.Log.d("CusCheckout", "Creating order: " + orderId);
        android.util.Log.d("CusCheckout", "Items count: " + items.size());
        android.util.Log.d("CusCheckout", "Items: " + items.toString());
        android.util.Log.d("CusCheckout", "Total: " + totalAmount);

        // Create order object
        Order order = new Order(orderId, customerId, customerName, items, totalAmount);
        order.setStatus("Pending");
        order.setCreatedAt(System.currentTimeMillis());
        order.setUpdatedAt(System.currentTimeMillis());

        // Save order to Firebase
        firebaseDatabase.getReference("orders").child(orderId)
                .setValue(order)
                .addOnSuccessListener(unused -> {
                    android.util.Log.d("CusCheckout", "Order saved successfully!");
                    Toast.makeText(CusCheckoutActivity.this, "Order placed successfully!", Toast.LENGTH_SHORT).show();
                    CartStore.clearCart(this);
                    startActivity(new Intent(this, CusHomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CusCheckout", "Failed to save order: " + e.getMessage());
                    confirmButton.setEnabled(true);
                    confirmButton.setText("Confirm Order");
                    Toast.makeText(CusCheckoutActivity.this,
                            "Failed to place order: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}