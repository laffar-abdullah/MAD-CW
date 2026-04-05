package com.example.buyngo.UI;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.buyngo.Model.Order;
import com.example.buyngo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 *                      CUSTOMER ORDER TRACKING ACTIVITY
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * WHAT THIS SCREEN DOES:\n * Shows all orders placed by logged-in customer with their current status.\n * Displays active orders (pending, being delivered) and completed orders.\n * \n * HOW IT CONNECTS TO FIREBASE:\n * 1. When screen opens, loadCustomerOrders() is called\n * 2. Reads ALL orders from Firebase /orders/ collection\n * 3. Filters to only show orders where customerId matches logged-in customer\n * 4. Uses LISTENER (not one-time read) so updates appear instantly\n * 5. When rider updates order status in their app, customer sees it real-time\n * \n * DATA FLOW:\n * Firebase /orders/ → Filter by customer ID → Display active/completed tabs\n *                                      ↓\n *                    Real-time listener watches for status changes\n *                                      ↓\n *                    When status changes, screen updates instantly\n * \n * ORDER STATUSES:\n * ACTIVE: Order Placed, Picked Up, On the Way, Delivered\n * COMPLETED: Received (customer confirmed), Delivered Successfully (feedback given)\n * \n * IMPORTANT:\n * - This is REAL-TIME - uses ValueEventListener not one-time query\n * - Customer only sees their own orders (filtered by customerId)\n * - When rider updates status, customer sees instantly (no refresh needed)\n * ═══════════════════════════════════════════════════════════════════════════════\n */\npublic class CusTrackingActivity extends AppCompatActivity {

    private LinearLayout ordersContainer;
    private FirebaseAuth firebaseAuth;              // Get logged-in customer
    private FirebaseDatabase firebaseDatabase;     // Read orders from Firebase
    private static final String TAG = "CusTracking";
    private List<Order> activeOrders = new ArrayList<>();
    private List<Order> completedOrders = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_tracking);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance("https://buyngo-5b43e-default-rtdb.firebaseio.com/");

        ordersContainer = findViewById(R.id.ordersContainer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Load customer orders from Firebase (with real-time updates)
        loadCustomerOrders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh orders when returning to this screen
        loadCustomerOrders();
    }

    /**
     * Load all orders for logged-in customer from Firebase with real-time updates
     */
    private void loadCustomerOrders() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String customerId = firebaseAuth.getCurrentUser().getUid();
        String customerEmail = firebaseAuth.getCurrentUser().getEmail();
        Log.d(TAG, "Loading orders for customer: " + customerId + " | Email: " + customerEmail);
        ordersContainer.removeAllViews();
        activeOrders.clear();
        completedOrders.clear();

        firebaseDatabase.getReference("orders")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        ordersContainer.removeAllViews();
                        activeOrders.clear();
                        completedOrders.clear();

                        if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                            TextView tvNoOrders = new TextView(CusTrackingActivity.this);
                            tvNoOrders.setText("No orders yet");
                            tvNoOrders.setTextSize(16);
                            tvNoOrders.setPadding(16, 16, 16, 16);
                            ordersContainer.addView(tvNoOrders);
                            return;
                        }

                        Log.d(TAG, "Total orders in Firebase: " + snapshot.getChildrenCount());
                        Log.d(TAG, "Current customer ID: " + customerId + " | Email: " + customerEmail);

                        for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                            try {
                                Order order = orderSnapshot.getValue(Order.class);

                                if (order != null) {
                                    String orderCustomerId = order.getCustomerId();
                                    String orderCustomerName = order.getCustomerName();

                                    Log.d(TAG, "Found order: " + order.getOrderId() +
                                            " | customerId: " + orderCustomerId +
                                            " | customerName: " + orderCustomerName +
                                            " | status: " + order.getStatus() +
                                            " | riderEmail: " + order.getAssignedRiderEmail());
                                    
                                    Log.d(TAG, "DEBUG: Current logged-in UID: '" + customerId + "' (length: " + customerId.length() + ")");
                                    Log.d(TAG, "DEBUG: Order customerId: '" + orderCustomerId + "' (length: " + (orderCustomerId != null ? orderCustomerId.length() : "null") + ")");

                                    // Categorize order if customerId matches current user
                                    if (orderCustomerId != null && customerId.equals(orderCustomerId)) {
                                        categorizeOrder(order);
                                        Log.d(TAG, "✓ Categorized order: " + order.getOrderId() + " (ID match)");
                                    } else {
                                        Log.d(TAG, "⊘ Skipping order (different customer): " + order.getOrderId() + " | Expected ID: " + customerId + " | Got: " + orderCustomerId);
                                        // FALLBACK: If customerId is null, still add it (in case not set properly in Firebase)
                                        if (orderCustomerId == null || orderCustomerId.isEmpty()) {
                                            Log.d(TAG, "⚠ Order has no customerId set. Adding to active orders as fallback.");
                                            categorizeOrder(order);
                                        }
                                    }
                                } else {
                                    Log.w(TAG, "Order is null from snapshot");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error loading order", e);
                                Toast.makeText(CusTrackingActivity.this,
                                        "Error loading order: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        if (activeOrders.isEmpty() && completedOrders.isEmpty()) {
                            TextView tvNoOrders = new TextView(CusTrackingActivity.this);
                            tvNoOrders.setText("No orders to track");
                            tvNoOrders.setTextSize(16);
                            tvNoOrders.setPadding(16, 16, 16, 16);
                            ordersContainer.addView(tvNoOrders);
                            Log.d(TAG, "No matching orders found for customer: " + customerId);
                        } else {
                            Log.d(TAG, "✓ Found " + activeOrders.size() + " active and " + completedOrders.size() + " completed orders for customer: " + customerId);
                            displayOrdersByCategory();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "Failed to load orders: " + error.getMessage());
                        Toast.makeText(CusTrackingActivity.this,
                                "Failed to load orders: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Categorize orders into active and completed
    // ACTIVE: Pending, Confirmed, Awaiting Pickup, Picked Up, On the Way, Delivered
    // COMPLETED: Received (customer confirmed), Delivered Successfully (customer skipped review)
    private void categorizeOrder(Order order) {
        String status = order.getStatus();
        
        // Only mark as completed when CUSTOMER has confirmed delivery
        // (Received = customer pressed "I have received", or Delivered Successfully = customer skipped review)
        if (status != null && (status.equals("Received") || status.equals("Delivered Successfully"))) {
            completedOrders.add(order);
        } else {
            // All other statuses are considered active (including "Delivered" which is rider's confirmation)
            activeOrders.add(order);
        }
        
        Log.d(TAG, "Categorized order " + order.getOrderId() + ": status='" + status + "' -> " + 
                (completedOrders.contains(order) ? "COMPLETED" : "ACTIVE"));
    }

    // Display orders organized by category with section headers
    private void displayOrdersByCategory() {
        // Display Active Orders section
        if (!activeOrders.isEmpty()) {
            addCategoryHeader("Active Orders");
            for (Order order : activeOrders) {
                displayOrderStatus(order);
            }
        }

        // Display Completed Orders section
        if (!completedOrders.isEmpty()) {
            addCategoryHeader("Completed Orders");
            for (Order order : completedOrders) {
                displayOrderStatus(order);
            }
        }
    }

    // Add a category header to the orders container
    private void addCategoryHeader(String categoryName) {
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        headerParams.setMargins(0, 16, 0, 8);
        headerLayout.setLayoutParams(headerParams);

        TextView tvCategoryHeader = new TextView(this);
        tvCategoryHeader.setText(categoryName);
        tvCategoryHeader.setTextSize(18);
        tvCategoryHeader.setTypeface(tvCategoryHeader.getTypeface(), Typeface.BOLD);
        tvCategoryHeader.setTextColor(getResources().getColor(R.color.primary_green, null));
        tvCategoryHeader.setPadding(16, 0, 16, 0);
        headerLayout.addView(tvCategoryHeader);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2));
        divider.setBackgroundColor(getResources().getColor(R.color.primary_green, null));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2);
        dividerParams.setMargins(16, 8, 16, 0);
        divider.setLayoutParams(dividerParams);
        headerLayout.addView(divider);

        ordersContainer.addView(headerLayout);
    }

    // Display order status and action buttons for a single order
    private void displayOrderStatus(Order order) {
        LinearLayout orderLayout = new LinearLayout(this);
        orderLayout.setOrientation(LinearLayout.VERTICAL);
        orderLayout.setPadding(16, 16, 16, 16);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        orderLayout.setLayoutParams(params);

        TextView tvOrderId = new TextView(this);
        tvOrderId.setText("Order #" + order.getOrderId());
        tvOrderId.setTextSize(18);
        tvOrderId.setTypeface(tvOrderId.getTypeface(), Typeface.BOLD);
        tvOrderId.setTextColor(getResources().getColor(R.color.primary_green, null));
        orderLayout.addView(tvOrderId);

        TextView tvStatus = new TextView(this);
        tvStatus.setText("Status: " + order.getStatus());
        tvStatus.setTextSize(16);
        tvStatus.setTextColor(getResources().getColor(R.color.text_dark, null));
        tvStatus.setPadding(0, 8, 0, 0);
        orderLayout.addView(tvStatus);

        TextView tvItems = new TextView(this);
        tvItems.setText("Items: " + order.getItemsAsString());
        tvItems.setTextSize(14);
        tvItems.setTextColor(getResources().getColor(R.color.text_light, null));
        tvItems.setPadding(0, 4, 0, 0);
        orderLayout.addView(tvItems);

        // Show total amount
        TextView tvTotal = new TextView(this);
        tvTotal.setText("Total: " + order.getTotalFormatted());
        tvTotal.setTextSize(14);
        tvTotal.setTypeface(tvTotal.getTypeface(), Typeface.BOLD);
        tvTotal.setTextColor(getResources().getColor(R.color.primary_green, null));
        tvTotal.setPadding(0, 4, 0, 0);
        orderLayout.addView(tvTotal);

        // Show rider details if order is confirmed and has a rider assigned
        if (order.getStatus() != null && (order.getStatus().equals("Confirmed") || order.getStatus().equals("Awaiting Pickup") || order.getStatus().equals("Picked Up") || order.getStatus().equals("On the Way") || order.getStatus().equals("Delivered"))) {
            if (order.getAssignedRiderEmail() != null && !order.getAssignedRiderEmail().isEmpty()) {
                TextView tvRiderLabel = new TextView(this);
                tvRiderLabel.setText("Assigned Rider:");
                tvRiderLabel.setTextSize(14);
                tvRiderLabel.setTypeface(tvRiderLabel.getTypeface(), Typeface.BOLD);
                tvRiderLabel.setTextColor(getResources().getColor(R.color.text_dark, null));
                tvRiderLabel.setPadding(0, 8, 0, 0);
                orderLayout.addView(tvRiderLabel);

                TextView tvRiderEmail = new TextView(this);
                tvRiderEmail.setText("Email: " + order.getAssignedRiderEmail());
                tvRiderEmail.setTextSize(13);
                tvRiderEmail.setTextColor(getResources().getColor(R.color.text_light, null));
                tvRiderEmail.setPadding(0, 4, 0, 0);
                orderLayout.addView(tvRiderEmail);

                if (order.getRiderName() != null && !order.getRiderName().isEmpty()) {
                    TextView tvRider = new TextView(this);
                    tvRider.setText("Name: " + order.getRiderName());
                    tvRider.setTextSize(13);
                    tvRider.setTextColor(getResources().getColor(R.color.text_light, null));
                    tvRider.setPadding(0, 2, 0, 0);
                    orderLayout.addView(tvRider);
                }

                // Show delivery status with color
                TextView tvDeliveryStatus = new TextView(this);
                tvDeliveryStatus.setText("Delivery Status: " + order.getStatus());
                tvDeliveryStatus.setTextSize(13);
                tvDeliveryStatus.setTypeface(tvDeliveryStatus.getTypeface(), Typeface.BOLD);
                tvDeliveryStatus.setTextColor(getStatusColor(order.getStatus()));
                tvDeliveryStatus.setPadding(0, 6, 0, 0);
                orderLayout.addView(tvDeliveryStatus);
            }
        } else if (order.getStatus() != null && order.getStatus().equals("Pending")) {
            TextView tvPendingInfo = new TextView(this);
            tvPendingInfo.setText("Status: Waiting for admin confirmation...");
            tvPendingInfo.setTextSize(13);
            tvPendingInfo.setTextColor(getResources().getColor(R.color.status_ordered, null));
            tvPendingInfo.setPadding(0, 4, 0, 0);
            orderLayout.addView(tvPendingInfo);
        }

        // Show "I have received" button ONLY when driver has marked order as Delivered
        if (order.getStatus() != null && order.getStatus().equals("Delivered")) {
            Button btnReceived = new Button(this);
            btnReceived.setText("I have received the order");
            btnReceived.setTextSize(14);
            btnReceived.setTextColor(getResources().getColor(R.color.white, null));
            btnReceived.setBackgroundColor(getResources().getColor(R.color.primary_green, null));
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            btnParams.setMargins(0, 12, 0, 0);
            btnReceived.setLayoutParams(btnParams);
            btnReceived.setOnClickListener(v -> updateOrderAndNavigateToFeedback(order.getOrderId()));
            orderLayout.addView(btnReceived);
        }

        // Show "Add Review" button if order has been Received
        if (order.getStatus() != null && order.getStatus().equals("Received")) {
            // Button container for review and skip buttons side by side
            LinearLayout reviewButtonsLayout = new LinearLayout(this);
            reviewButtonsLayout.setOrientation(LinearLayout.HORIZONTAL);
            reviewButtonsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            reviewButtonsLayout.setPadding(0, 12, 0, 0);

            // Add Review button
            Button btnFeedback = new Button(this);
            btnFeedback.setText("Add Review");
            btnFeedback.setTextSize(13);
            btnFeedback.setTextColor(getResources().getColor(R.color.white, null));
            btnFeedback.setBackgroundColor(getResources().getColor(R.color.primary_green, null));
            LinearLayout.LayoutParams feedbackParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1);
            feedbackParams.setMargins(0, 0, 6, 0);
            btnFeedback.setLayoutParams(feedbackParams);
            btnFeedback.setOnClickListener(v -> {
                Intent intent = new Intent(CusTrackingActivity.this, CusFeedbackActivity.class);
                intent.putExtra("orderId", order.getOrderId());
                intent.putExtra("mandatory", false);  // Feedback is now optional
                startActivity(intent);
            });
            reviewButtonsLayout.addView(btnFeedback);

            // Skip Review button
            Button btnSkipReview = new Button(this);
            btnSkipReview.setText("Skip");
            btnSkipReview.setTextSize(13);
            btnSkipReview.setTextColor(getResources().getColor(R.color.text_dark, null));
            btnSkipReview.setBackgroundColor(getResources().getColor(R.color.divider, null));
            LinearLayout.LayoutParams skipParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1);
            skipParams.setMargins(6, 0, 0, 0);
            btnSkipReview.setLayoutParams(skipParams);
            btnSkipReview.setOnClickListener(v -> {
                // Mark order as "Delivered Successfully" without review
                firebaseDatabase.getReference("orders")
                        .child(order.getOrderId())
                        .child("status")
                        .setValue("Delivered Successfully")
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(CusTrackingActivity.this,
                                    "Order marked as complete!",
                                    Toast.LENGTH_SHORT).show();
                            loadCustomerOrders();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(CusTrackingActivity.this,
                                    "Failed to update order: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
            });
            reviewButtonsLayout.addView(btnSkipReview);

            orderLayout.addView(reviewButtonsLayout);
        }

        // Show "Order Delivered Successfully" status when review is completed
        if (order.getStatus() != null && order.getStatus().equals("Delivered Successfully")) {
            TextView tvDeliveredStatus = new TextView(this);
            tvDeliveredStatus.setText("✓ Order Delivered Successfully");
            tvDeliveredStatus.setTextSize(14);
            tvDeliveredStatus.setTypeface(null, android.graphics.Typeface.BOLD);
            tvDeliveredStatus.setTextColor(getResources().getColor(R.color.primary_green, null));
            tvDeliveredStatus.setPadding(0, 12, 0, 0);
            orderLayout.addView(tvDeliveredStatus);
        }

        View separator = new View(this);
        separator.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2));
        separator.setBackgroundColor(getResources().getColor(R.color.divider, null));
        LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2);
        sepParams.setMargins(0, 16, 0, 0);
        separator.setLayoutParams(sepParams);

        ordersContainer.addView(orderLayout);
        ordersContainer.addView(separator);
    }

    // Update order status to "Received" and refresh the list
    private void updateOrderAndNavigateToFeedback(String orderId) {
        firebaseDatabase.getReference("orders")
                .child(orderId)
                .child("status")
                .setValue("Received")
                .addOnSuccessListener(unused -> {
                    Toast.makeText(CusTrackingActivity.this,
                            "Order received! You can now add a review (optional).",
                            Toast.LENGTH_SHORT).show();
                    loadCustomerOrders();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CusTrackingActivity.this,
                            "Failed to update order: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Returns the appropriate color for a given delivery status
     */
    private int getStatusColor(String status) {
        if (status == null) {
            return getResources().getColor(R.color.text_dark, null);
        }

        switch (status) {
            case "Pending":
                return getResources().getColor(R.color.status_ordered, null);
            case "Confirmed":
            case "Awaiting Pickup":
                return getResources().getColor(R.color.status_ordered, null);
            case "Picked Up":
                return getResources().getColor(R.color.status_picked, null);
            case "On the Way":
                return getResources().getColor(R.color.status_out_for_delivery, null);
            case "Delivered":
                return getResources().getColor(R.color.status_delivered, null);
            case "Received":
            case "Delivered Successfully":
                return getResources().getColor(R.color.primary_green, null);
            default:
                return getResources().getColor(R.color.text_dark, null);
        }
    }
}