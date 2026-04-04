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

public class CusTrackingActivity extends AppCompatActivity {

    private LinearLayout ordersContainer;
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private static final String TAG = "CusTracking";
    private List<Order> activeOrders = new ArrayList<>();
    private List<Order> completedOrders = new ArrayList<>();
    private String currentCustomerEmail = "";

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

        loadCustomerOrders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomerOrders();
    }

    // Load all orders for the logged-in customer from Firebase
    private void loadCustomerOrders() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String customerId = firebaseAuth.getCurrentUser().getUid();
        String customerEmail = firebaseAuth.getCurrentUser().getEmail();
        currentCustomerEmail = customerEmail != null ? customerEmail : "";

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

                                    // Try matching by customerId first
                                    boolean isCustomerOrder = (orderCustomerId != null && customerId.equals(orderCustomerId));

                                    // Fallback: try matching by customer name if customerId is null
                                    if (!isCustomerOrder && orderCustomerId == null && orderCustomerName != null) {
                                        Log.d(TAG, "⚠ Order has no customerId. Attempting to match by other criteria...");
                                        // Display order if customerId is not set (legacy orders)
                                        isCustomerOrder = true;
                                    }

                                    if (isCustomerOrder) {
                                        categorizeOrder(order);
                                        Log.d(TAG, "✓ Categorized order: " + order.getOrderId());
                                    } else {
                                        Log.d(TAG, "⊘ Skipping order (different customer): " + order.getOrderId() + " | Expected ID: " + customerId + " | Got: " + orderCustomerId);
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

    // Categorize orders into active and completed based on rider status updates
    private void categorizeOrder(Order order) {
        String status = order.getStatus();

        // Completed statuses: Final delivery states after customer confirms receipt
        if (status != null && (status.equals("Delivered Successfully") ||
                status.equals("Received") ||
                status.equals("Completed"))) {
            completedOrders.add(order);
        }
        // Active statuses: All stages from order creation through rider delivery
        // - "Pending": Awaiting admin confirmation
        // - "Confirmed": Admin approved, awaiting rider
        // - "Awaiting Pickup": Rider assigned, ready to pick up
        // - "Picked Up": Rider picked up items
        // - "On the Way": Rider en route to customer
        // - "Delivered": Rider completed delivery (customer hasn't marked received yet)
        else if (status != null &&
                (status.equals("Pending") ||
                        status.equals("Confirmed") ||
                        status.equals("Awaiting Pickup") ||
                        status.equals("Picked Up") ||
                        status.equals("On the Way") ||
                        status.equals("Delivered"))) {
            activeOrders.add(order);
        }
        else {
            // Fallback: treat as active if status is unknown or null
            Log.w(TAG, "Unknown order status: " + status + " for order " + order.getOrderId());
            activeOrders.add(order);
        }
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