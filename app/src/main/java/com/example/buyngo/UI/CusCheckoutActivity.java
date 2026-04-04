package com.example.buyngo.UI;

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

public class CusCheckoutActivity extends AppCompatActivity {

    // Views for order summary
    private TextView totalPriceText;

    // Delivery address fields
    private EditText phoneEditText;
    private EditText addressEditText;
    private EditText cityEditText;

    // Payment method radio buttons
    private RadioGroup paymentRadioGroup;
    private RadioButton radioCard;
    private RadioButton radioCOD;

    // Card details section (only shown when "Card" is selected)
    private LinearLayout cardDetailsLayout;
    private EditText cardNumberEditText;
    private EditText cardExpiryEditText;
    private EditText cardCvvEditText;

    // The big "Confirm Order" button at the bottom
    private Button confirmButton;

    // Firebase connections
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_checkout);

        // Set up Firebase auth and database connections
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        // Set up the top toolbar with a back arrow
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Bind all views from the layout file
        bindViews();

        // Pre-fill address from customer's saved profile so they don't retype it
        loadCustomerAddress();

        // Show the cart total at the top of the screen
        updateTotalPrice();

        // **FIXED: Set up payment method toggle with proper initialization**
        setupPaymentMethodToggle();

        // When the customer taps "Confirm Order", start the order placement flow
        confirmButton.setOnClickListener(v -> placeOrder());
    }

    /**
     * Bind all views safely with null checks
     */
    private void bindViews() {
        totalPriceText = findViewById(R.id.totalPriceText);
        phoneEditText = findViewById(R.id.phoneEditText);
        addressEditText = findViewById(R.id.addressEditText);
        cityEditText = findViewById(R.id.cityEditText);
        paymentRadioGroup = findViewById(R.id.paymentRadioGroup);
        radioCard = findViewById(R.id.radioCard);
        radioCOD = findViewById(R.id.radioCOD);
        cardDetailsLayout = findViewById(R.id.cardDetailsLayout);
        cardNumberEditText = findViewById(R.id.cardNumberEditText);
        cardExpiryEditText = findViewById(R.id.cardExpiryEditText);
        cardCvvEditText = findViewById(R.id.cardCvvEditText);
        confirmButton = findViewById(R.id.confirmButton);

        // Safety check - make sure all critical views are found
        if (confirmButton == null) {
            Toast.makeText(this, "Error: Confirm button not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * **NEW: Properly set up payment method toggle with initial state handling**
     */
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

    /**
     * **NEW: Helper method to show/hide card fields based on selection**
     */
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

    /**
     * Update the total price display
     */
    private void updateTotalPrice() {
        if (totalPriceText != null) {
            double total = CartStore.getCartTotal(this);
            totalPriceText.setText(String.format("Total: Rs. %.2f", total));
        }
    }

    /**
     * Reads the customer's saved address and phone number from Firebase
     * and fills in the form fields automatically.
     */
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

    /**
     * Validates all fields and the payment details, then triggers order creation.
     */
    private void placeOrder() {
        // Validate address fields
        if (!validateAddressFields()) {
            return;
        }

        // Validate payment method
        String paymentMethod = getSelectedPaymentMethod();
        if (paymentMethod == null) {
            Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate card details if card is selected
        if ("Card".equals(paymentMethod) && !validateCardDetails()) {
            return;
        }

        // Disable button during processing
        confirmButton.setEnabled(false);
        confirmButton.setText("Placing Order...");

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            resetConfirmButton();
            return;
        }

        String userId = firebaseAuth.getCurrentUser().getUid();
        List<CartStore.CartItem> cartItems = CartStore.getCartItems(this);

        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
            resetConfirmButton();
            return;
        }

        // Get customer profile and create order
        firebaseDatabase.getReference("users").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            User user = snapshot.getValue(User.class);
                            if (user != null) {
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

    /**
     * **NEW: Helper methods for validation**
     */
    private boolean validateAddressFields() {
        String phone = phoneEditText != null ? phoneEditText.getText().toString().trim() : "";
        String address = addressEditText != null ? addressEditText.getText().toString().trim() : "";
        String city = cityEditText != null ? cityEditText.getText().toString().trim() : "";

        if (phone.isEmpty() || address.isEmpty() || city.isEmpty()) {
            Toast.makeText(this, "Please fill in all address fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

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

    /**
     * Builds the Order object and writes it to Firebase.
     */
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