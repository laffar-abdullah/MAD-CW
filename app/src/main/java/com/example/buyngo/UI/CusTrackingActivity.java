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

public class CusTrackingActivity extends AppCompatActivity {

    private LinearLayout ordersContainer;
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private static final String TAG = "CusTracking";

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
        Log.d(TAG, "Loading orders for customer: " + customerId);
        ordersContainer.removeAllViews();

        firebaseDatabase.getReference("orders")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        ordersContainer.removeAllViews();

                        if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                            TextView tvNoOrders = new TextView(CusTrackingActivity.this);
                            tvNoOrders.setText("No orders yet");
                            tvNoOrders.setTextSize(16);
                            tvNoOrders.setPadding(16, 16, 16, 16);
                            ordersContainer.addView(tvNoOrders);
                            return;
                        }

                        Log.d(TAG, "Total orders in Firebase: " + snapshot.getChildrenCount());
                        boolean foundOrder = false;

                        for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                            try {
                                Order order = orderSnapshot.getValue(Order.class);

                                if (order != null) {
                                    Log.d(TAG, "Found order: " + order.getOrderId() + ", customerId: " + order.getCustomerId() + ", status: " + order.getStatus());
                                    if (customerId.equals(order.getCustomerId())) {
                                        foundOrder = true;
                                        Log.d(TAG, "Displaying order: " + order.getOrderId());
                                        displayOrderStatus(order);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error loading order", e);
                                Toast.makeText(CusTrackingActivity.this,
                                        "Error loading order: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        if (!foundOrder) {
                            TextView tvNoOrders = new TextView(CusTrackingActivity.this);
                            tvNoOrders.setText("No orders to track");
                            tvNoOrders.setTextSize(16);
                            tvNoOrders.setPadding(16, 16, 16, 16);
                            ordersContainer.addView(tvNoOrders);
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

        // Show rider details if order is confirmed and has a rider assigned
        if (order.getStatus() != null && (order.getStatus().equals("Confirmed") || order.getStatus().equals("Awaiting Pickup") || order.getStatus().equals("Delivered"))) {
            if (order.getRiderId() != null && !order.getRiderId().isEmpty()) {
                TextView tvRiderLabel = new TextView(this);
                tvRiderLabel.setText("Assigned Rider:");
                tvRiderLabel.setTextSize(14);
                tvRiderLabel.setTypeface(tvRiderLabel.getTypeface(), Typeface.BOLD);
                tvRiderLabel.setTextColor(getResources().getColor(R.color.text_dark, null));
                tvRiderLabel.setPadding(0, 8, 0, 0);
                orderLayout.addView(tvRiderLabel);

                TextView tvRider = new TextView(this);
                tvRider.setText("Name: " + (order.getRiderName() != null ? order.getRiderName() : "Assigned"));
                tvRider.setTextSize(13);
                tvRider.setTextColor(getResources().getColor(R.color.text_light, null));
                tvRider.setPadding(0, 4, 0, 0);
                orderLayout.addView(tvRider);
            }
        } else if (order.getStatus() != null && order.getStatus().equals("Pending")) {
            TextView tvPendingInfo = new TextView(this);
            tvPendingInfo.setText("Waiting for admin confirmation...");
            tvPendingInfo.setTextSize(13);
            tvPendingInfo.setTextColor(getResources().getColor(R.color.status_ordered, null));
            tvPendingInfo.setPadding(0, 4, 0, 0);
            orderLayout.addView(tvPendingInfo);
        }

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

        final String orderId = order.getOrderId();
        final String currentStatus = order.getStatus();
        
        // Show "I have received the order" button only if order is Delivered
        boolean isDelivered = currentStatus != null && currentStatus.equals("Delivered");
        if (!isDelivered) {
            btnReceived.setVisibility(View.GONE);
        }

        btnReceived.setOnClickListener(v -> updateOrderAndNavigateToFeedback(orderId));
        orderLayout.addView(btnReceived);

        // Show "Add Feedback" button if order has been Received (to go back to feedback)
        if (currentStatus != null && currentStatus.equals("Received")) {
            Button btnFeedback = new Button(this);
            btnFeedback.setText("Add Feedback");
            btnFeedback.setTextSize(14);
            btnFeedback.setTextColor(getResources().getColor(R.color.white, null));
            btnFeedback.setBackgroundColor(getResources().getColor(R.color.primary_green, null));
            LinearLayout.LayoutParams feedbackParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            feedbackParams.setMargins(0, 12, 0, 0);
            btnFeedback.setLayoutParams(feedbackParams);
            btnFeedback.setOnClickListener(v -> {
                Intent intent = new Intent(CusTrackingActivity.this, CusFeedbackActivity.class);
                intent.putExtra("orderId", orderId);
                startActivity(intent);
            });
            orderLayout.addView(btnFeedback);
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

    // Update order status to "Received" and navigate to feedback
    private void updateOrderAndNavigateToFeedback(String orderId) {
        firebaseDatabase.getReference("orders")
                .child(orderId)
                .child("status")
                .setValue("Received")
                .addOnSuccessListener(unused -> {
                    Toast.makeText(CusTrackingActivity.this,
                            "Order received! Please add a review.",
                            Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(CusTrackingActivity.this, CusFeedbackActivity.class);
                    intent.putExtra("orderId", orderId);
                    intent.putExtra("mandatory", true);  // Feedback is mandatory after order received
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CusTrackingActivity.this,
                            "Failed to update order: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}