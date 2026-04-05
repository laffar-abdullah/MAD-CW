ckage com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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

    private void setupPaymentMethodToggle() {
        if (paymentRadioGroup == null || radioCard == null || radioCOD == null) {
            android.util.Log.e("Checkout", "Payment radio buttons not found!");
            return;
        }

        // Set initial state (Card is selected by default in XML)
        updateCardVisibility();

        // Listen for payment method changes
        paymentRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                android.util.Log.d("Checkout", "Payment method changed to: " + checkedId);
                updateCardVisibility();
            }
        });
    }

    
    private void updateCardVisibility() {
        if (radioCard != null && radioCard.isChecked()) {
            // Card selected - show card details
            if (cardDetailsLayout != null) {
                cardDetailsLayout.setVisibility(View.VISIBLE);
            }
        } else {
            // COD selected - hide card details
            if (cardDetailsLayout != null) {
                cardDetailsLayout.setVisibility(View.GONE);
            }
        }
    }

    
    private void updateTotalPrice() {
        if (totalPriceText != null) {
            double total = CartStore.getCartTotal(this);
            totalPriceText.setText(String.format("Total: Rs. %.2f", total));
        }
    }

    
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
                        if (snapshot.exists() && phoneEditText != null &&
                                addressEditText != null && cityEditText != null) {
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
                                "Could not load your saved address",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    
    // When customer clicks "Confirm Order" button
    private void placeOrder() {
        // Step 1: Check if customer entered delivery address
        if (!validateAddressFields()) {
            return;
        }

        // Step 2: Check if customer selected payment method (Card or Cash)
        String paymentMethod = getSelectedPaymentMethod();
        if (paymentMethod == null) {
            Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
            return;
        }

        // Step 3: If Card selected, check if card details are valid
        if ("Card".equals(paymentMethod) && !validateCardDetails()) {
            return;
        }

        // Step 4: Disable button while creating order (prevent double-click)
        confirmButton.setEnabled(false);
        confirmButton.setText("Placing Order...");

        // Step 5: Check if customer is logged in
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            resetConfirmButton();
            return;
        }

        // Step 6: Get customer ID from Firebase
        String userId = firebaseAuth.getCurrentUser().getUid();
        
        // Step 7: Get all items from shopping cart (stored in local phone storage)
        List<CartStore.CartItem> cartItems = CartStore.getCartItems(this);

        // Step 8: Check if cart has items, if empty don't allow order
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
            resetConfirmButton();
            return;
        }

        // Step 9: Get customer's saved profile from Firebase database
        firebaseDatabase.getReference("users").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        // Step 10: Check if customer profile exists
                        if (snapshot.exists()) {
                            // Step 11: Convert Firebase data to User object
                            User user = snapshot.getValue(User.class);
                            if (user != null) {
                                // Step 12: Create Order object and save to Firebase
                                createAndSaveOrder(userId, user.getFullName(),
                                        cartItems, paymentMethod);
                            } else {
                                resetConfirmButton();
                            }
                        } else {
                            resetConfirmButton();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        resetConfirmButton();
                        Toast.makeText(CusCheckoutActivity.this,
                                "Could not get profile info",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Check if all address fields are filled
    private boolean validateAddressFields() {
        String phone = phoneEditText != null ? phoneEditText.getText().toString().trim() : "";
        String address = addressEditText != null ? addressEditText.getText().toString().trim() : "";
        String city = cityEditText != null ? cityEditText.getText().toString().trim() : "";

        // If any field is empty, show error
        if (phone.isEmpty() || address.isEmpty() || city.isEmpty()) {
            Toast.makeText(this, "Please fill in all address fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // Get which payment method customer selected
    private String getSelectedPaymentMethod() {
        if (radioCard != null && radioCard.isChecked()) {
            return "Card";
        } else if (radioCOD != null && radioCOD.isChecked()) {
            return "Cash on Delivery";
        }
        return null;
    }

    private boolean validateCardDetails() {
        String cardNum = cardNumberEditText != null ? cardNumberEditText.getText().toString().trim() : "";
        String expiry = cardExpiryEditText != null ? cardExpiryEditText.getText().toString().trim() : "";
        String cvv = cardCvvEditText != null ? cardCvvEditText.getText().toString().trim() : "";

        if (cardNum.isEmpty() || expiry.isEmpty() || cvv.isEmpty()) {
            Toast.makeText(this, "Please fill in all card details", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (cardNum.replaceAll("\\s", "").length() < 16) {
            Toast.makeText(this, "Please enter a valid 16-digit card number", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void resetConfirmButton() {
        confirmButton.setEnabled(true);
        confirmButton.setText("Confirm Order");
    }

    
    private void createAndSaveOrder(String customerId, String customerName,
                                    List<CartStore.CartItem> cartItems,
                                    String paymentMethod) {
        String orderId = "ORD-" + System.currentTimeMillis();
        Map<String, Integer> items = new HashMap<>();
        for (CartStore.CartItem item : cartItems) {
            items.put(item.name, item.quantity);
        }

        double totalAmount = CartStore.getCartTotal(this);
        String customerAddr = addressEditText != null ? addressEditText.getText().toString().trim() : "Address not provided";
        String customerPhone = phoneEditText != null ? phoneEditText.getText().toString().trim() : "Phone not provided";

        Order order = new Order(orderId, customerId, customerName, items, totalAmount);
        order.setStatus("Pending");
        order.setCreatedAt(System.currentTimeMillis());
        order.setUpdatedAt(System.currentTimeMillis());
        order.setCustomerAddress(customerAddr);
        order.setCustomerPhone(customerPhone);
        order.setPaymentMethod(paymentMethod);

        firebaseDatabase.getReference("orders").child(orderId)
                .setValue(order)
                .addOnSuccessListener(unused -> {
                    String msg = "Card".equals(paymentMethod)
                            ? "Order placed! Payment via card confirmed ✓"
                            : "Order placed! Please pay Rs. "
                            + String.format("%.2f", totalAmount)
                            + " cash to the rider.";

                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    CartStore.clearCart(this);
                    startActivity(new Intent(this, CusHomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    resetConfirmButton();
                    Toast.makeText(this, "Failed to place order", Toast.LENGTH_SHORT).show();
                });
    }
}
