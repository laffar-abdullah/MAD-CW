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

        loadOrdersFromFirebase();
    }

    private void loadOrdersFromFirebase() {
        ordersContainer.removeAllViews();

        firebaseDatabase.getReference("orders")
                .addListenerForSingleValueEvent(new ValueEventListener() {
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

                        Log.d(TAG, "Found " + snapshot.getChildrenCount() + " orders");

                        for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                            try {
                                Log.d(TAG, "Order data: " + orderSnapshot.getValue());
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
                });
    }

    private void inflateOrderCard(Order order, String customerName, String customerAddress) {
        CardView cardView = (CardView) layoutInflater
                .inflate(R.layout.item_order_card, ordersContainer, false);

        TextView tvOrderId = cardView.findViewById(R.id.tvOrderId);
        TextView tvCustomerName = cardView.findViewById(R.id.tvCustomerName);
        TextView tvItemsAndTotal = cardView.findViewById(R.id.tvItemsAndTotal);
        TextView tvStatus = cardView.findViewById(R.id.tvStatus);
        Button btnConfirmOrder = cardView.findViewById(R.id.btnConfirmOrder);
        Button btnAssignRider = cardView.findViewById(R.id.btnAssignRider);

        tvOrderId.setText("Order #" + order.getOrderId());
        tvCustomerName.setText("Customer: " + customerName);
        tvItemsAndTotal.setText("Items: " + order.getItemsAsString() + "  |  Total: " + order.getTotalFormatted());
        tvStatus.setText("Status: " + (order.getStatus() == null ? "Pending" : order.getStatus()));

        // Hide confirm button if order is already confirmed
        if (order.getStatus() != null && order.getStatus().equals("Confirmed")) {
            btnConfirmOrder.setVisibility(View.GONE);
        } else {
            btnConfirmOrder.setOnClickListener(v -> confirmOrder(order.getOrderId(), tvStatus, btnConfirmOrder));
        }

        btnAssignRider.setOnClickListener(v ->
                openAssignRider(order.getOrderId(), customerName, customerAddress));

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
        intent.putExtra(EXTRA_ORDER_ID, orderId);
        intent.putExtra(EXTRA_CUSTOMER_NAME, customerName);
        intent.putExtra(EXTRA_CUSTOMER_ADDRESS, customerAddress);
        // Backward-compat key in case other paths still read this.
        intent.putExtra("orderId", orderId);
        startActivity(intent);
    }
}