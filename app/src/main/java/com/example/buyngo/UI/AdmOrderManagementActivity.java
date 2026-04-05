package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.example.buyngo.Model.Order;
import com.example.buyngo.R;
import com.example.buyngo.Utils.FirebaseOrderCleanup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdmOrderManagementActivity extends AppCompatActivity {
    private static final String EXTRA_ORDER_ID = "extra_order_id";
    private static final String EXTRA_CUSTOMER_NAME = "extra_customer_name";
    private static final String EXTRA_CUSTOMER_ADDRESS = "extra_customer_address";
    private static final String TAG = "AdmOrderMgmt";
    private LinearLayout ordersContainer;
    private FirebaseDatabase firebaseDatabase;
    private LayoutInflater layoutInflater;
    private ValueEventListener ordersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adm_order_management);

        firebaseDatabase = FirebaseDatabase.getInstance("https://buyngo-5b43e-default-rtdb.firebaseio.com/");
        layoutInflater = LayoutInflater.from(this);
        ordersContainer = findViewById(R.id.ordersContainer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Add a "Clear All Orders" button
        addClearAllOrdersButton();

        // Clean up old demo orders on app load
        FirebaseOrderCleanup.cleanupOldOrders();

        loadOrdersFromFirebase();
    }

    /**
     * Adds a "Clear All Orders" button to the toolbar menu
     */
    private void addClearAllOrdersButton() {
        TextView btnClearAll = new TextView(this);
        btnClearAll.setText("Clear All");
        btnClearAll.setTextSize(16);
        btnClearAll.setTextColor(getResources().getColor(R.color.white, null));
        btnClearAll.setPadding(20, 10, 20, 10);
        btnClearAll.setOnClickListener(v -> showClearAllConfirmation());
        
        // Add button to toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.addView(btnClearAll);
    }

    /**
     * Shows a confirmation dialog before clearing all orders
     */
    private void showClearAllConfirmation() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Clear All Orders")
                .setMessage("Are you sure you want to delete all orders? This action cannot be undone.")
                .setPositiveButton("Yes, Delete All", (dialog, which) -> clearAllOrders())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Clears all orders from Firebase
     */
    private void clearAllOrders() {
        firebaseDatabase.getReference("orders").removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "All orders cleared successfully!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "All orders have been deleted from Firebase");
                    ordersContainer.removeAllViews();
                    
                    TextView tvNoOrders = new TextView(this);
                    tvNoOrders.setText("No orders available");
                    tvNoOrders.setTextSize(16);
                    tvNoOrders.setPadding(16, 16, 16, 16);
                    ordersContainer.addView(tvNoOrders);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to clear orders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to clear all orders: " + e.getMessage());
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload orders when activity is resumed
        loadOrdersFromFirebase();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Remove the listener when activity pauses to prevent stacking listeners
        if (ordersListener != null) {
            firebaseDatabase.getReference("orders").removeEventListener(ordersListener);
        }
    }

    private void loadOrdersFromFirebase() {
        // Remove any existing listener first to prevent duplicates
        if (ordersListener != null) {
            firebaseDatabase.getReference("orders").removeEventListener(ordersListener);
        }

        ordersContainer.removeAllViews();

        ordersListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        ordersContainer.removeAllViews();

                        if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                            TextView tvNoOrders = new TextView(AdmOrderManagementActivity.this);
                            tvNoOrders.setText("No orders available");
                            tvNoOrders.setTextSize(16);
                            tvNoOrders.setPadding(16, 16, 16, 16);
                            ordersContainer.addView(tvNoOrders);
                            return;
                        }

                        Log.d(TAG, "Found " + snapshot.getChildrenCount() + " total orders");

                        for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                            try {
                                Order order = orderSnapshot.getValue(Order.class);
                                if (order != null) {
                                    String customerName = order.getCustomerName() == null
                                            ? "Customer"
                                            : order.getCustomerName();
                                    String customerAddress = orderSnapshot.child("customerAddress")
                                            .getValue(String.class);
                                    if (customerAddress == null || customerAddress.trim().isEmpty()) {
                                        customerAddress = "Address unavailable";
                                    }
                                    
                                    Log.d(TAG, "Loading order: " + order.getOrderId() + 
                                          ", items: " + (order.getItems() != null ? order.getItems().size() : 0) +
                                          ", total: " + order.getTotalAmount());
                                    
                                    inflateOrderCard(order, customerName, customerAddress);
                                } else {
                                    Log.w(TAG, "Order is null from snapshot");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error loading order: " + e.getMessage(), e);
                                Toast.makeText(AdmOrderManagementActivity.this,
                                        "Error loading order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "Failed to load orders: " + error.getMessage());
                        Toast.makeText(AdmOrderManagementActivity.this,
                                "Failed to load orders: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                };
        
        // Attach the listener
        firebaseDatabase.getReference("orders").addValueEventListener(ordersListener);
    }

    private void inflateOrderCard(Order order, String customerName, String customerAddress) {
        CardView cardView = (CardView) layoutInflater
                .inflate(R.layout.item_order_card, ordersContainer, false);

        TextView tvOrderId = cardView.findViewById(R.id.tvOrderId);
        TextView tvCustomerName = cardView.findViewById(R.id.tvCustomerName);
        TextView tvItemsAndTotal = cardView.findViewById(R.id.tvItemsAndTotal);
        TextView tvStatus = cardView.findViewById(R.id.tvStatus);
        TextView tvAssignedRider = cardView.findViewById(R.id.tvAssignedRider);
        Button btnConfirmOrder = cardView.findViewById(R.id.btnConfirmOrder);
        Button btnAssignRider = cardView.findViewById(R.id.btnAssignRider);

        tvOrderId.setText("Order #" + order.getOrderId());
        tvCustomerName.setText("Customer: " + customerName);
        
        // Build items string with quantities
        String itemsDisplay = order.getItemsAsString();
        String totalDisplay = order.getTotalFormatted();
        
        Log.d(TAG, "Order: " + order.getOrderId() + 
              " | Items: " + itemsDisplay + 
              " | Total: " + totalDisplay + 
              " | Items Map: " + (order.getItems() != null ? order.getItems().toString() : "null") +
              " | Total Amount: " + order.getTotalAmount());
        
        tvItemsAndTotal.setText("Items: " + itemsDisplay + "  |  Total: " + totalDisplay);
        tvStatus.setText("Status: " + (order.getStatus() == null ? "Pending" : order.getStatus()));

        // Show assigned rider if available
        if (order.getAssignedRiderEmail() != null && !order.getAssignedRiderEmail().isEmpty()) {
            tvAssignedRider.setText("Assigned Rider: " + order.getAssignedRiderEmail());
            tvAssignedRider.setVisibility(View.VISIBLE);
        } else {
            tvAssignedRider.setVisibility(View.GONE);
        }

        // Workflow: Assign Rider FIRST, then Confirm
        if (order.getStatus() == null || order.getStatus().equals("Pending")) {
            // For Pending orders, check if rider is already assigned
            
            if (order.getAssignedRiderEmail() == null || order.getAssignedRiderEmail().isEmpty()) {
                // NO RIDER ASSIGNED YET - Show ONLY "Assign Rider" button
                Log.d(TAG, "Order " + order.getOrderId() + " is Pending with NO rider - showing assign rider button");
                btnAssignRider.setVisibility(View.VISIBLE);
                btnAssignRider.setEnabled(true);
                btnAssignRider.setOnClickListener(v ->
                        openAssignRider(order.getOrderId(), customerName, customerAddress));
                
                // Hide confirm button until rider is assigned
                btnConfirmOrder.setVisibility(View.GONE);
            } else {
                // RIDER ALREADY ASSIGNED - Show ONLY "Confirm" button
                Log.d(TAG, "Order " + order.getOrderId() + " has rider assigned (" + order.getAssignedRiderEmail() + ") - showing confirm button");
                btnConfirmOrder.setVisibility(View.VISIBLE);
                btnConfirmOrder.setEnabled(true);
                btnConfirmOrder.setOnClickListener(v -> confirmOrder(order.getOrderId(), tvStatus, btnConfirmOrder));
                
                // Hide assign rider button - already assigned
                btnAssignRider.setVisibility(View.GONE);
            }
        } else {
            // Order is Confirmed or beyond - Hide both buttons
            Log.d(TAG, "Order " + order.getOrderId() + " status is " + order.getStatus() + " - hiding all buttons");
            btnConfirmOrder.setVisibility(View.GONE);
            btnAssignRider.setVisibility(View.GONE);
        }

        ordersContainer.addView(cardView);
    }

    private void confirmOrder(String orderId, TextView tvStatus, Button btnConfirmOrder) {
        firebaseDatabase.getReference("orders").child(orderId)
                .child("status").setValue("Confirmed")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Order Confirmed!", Toast.LENGTH_SHORT).show();
                    tvStatus.setText("Status: Confirmed");
                    btnConfirmOrder.setVisibility(View.GONE);
                    Log.d(TAG, "Order " + orderId + " confirmed and button hidden");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to confirm order", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to confirm order: " + e.getMessage());
                });
    }


    private void openAssignRider(String orderId, String customerName, String customerAddress) {
        Intent intent = new Intent(this, AdmAssignRiderActivity.class);
        intent.putExtra("extra_order_id", orderId);
        intent.putExtra("extra_customer_name", customerName);
        intent.putExtra("extra_customer_address", customerAddress);
        startActivity(intent);
    }
}