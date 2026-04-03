package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.buyngo.Model.Order;
import com.example.buyngo.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdmOrderManagementActivity extends AppCompatActivity {
<<<<<<< HEAD

    private static final String EXTRA_ORDER_ID = "extra_order_id";
    private static final String EXTRA_CUSTOMER_NAME = "extra_customer_name";
    private static final String EXTRA_CUSTOMER_ADDRESS = "extra_customer_address";
=======
    private LinearLayout ordersContainer;
    private FirebaseDatabase firebaseDatabase;
    private LayoutInflater layoutInflater;
>>>>>>> cc33148f16efd7cd1a6422a65c9b53b87be2e710

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adm_order_management);

        firebaseDatabase = FirebaseDatabase.getInstance();
        layoutInflater = LayoutInflater.from(this);
        ordersContainer = findViewById(R.id.ordersContainer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

<<<<<<< HEAD
        findViewById(R.id.btnAssignRider).setOnClickListener(v ->
            openAssignRider("BNG-001", "Alice Smith", "12 Main Street, Springfield"));

        findViewById(R.id.btnAssignRiderOrder2).setOnClickListener(v ->
            openAssignRider("BNG-002", "Bob Jones", "78 Hill Road, Brookfield"));

        findViewById(R.id.btnAssignRiderOrder3).setOnClickListener(v ->
            openAssignRider("BNG-003", "Carol Lee", "45 Lake Avenue, Maple Town"));
        }

        private void openAssignRider(String orderId, String customerName, String customerAddress) {
        Intent intent = new Intent(this, AdmAssignRiderActivity.class);
        intent.putExtra(EXTRA_ORDER_ID, orderId);
        intent.putExtra(EXTRA_CUSTOMER_NAME, customerName);
        intent.putExtra(EXTRA_CUSTOMER_ADDRESS, customerAddress);
        startActivity(intent);
=======
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

                        for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                            try {
                                Order order = orderSnapshot.getValue(Order.class);
                                if (order != null) {
                                    inflateOrderCard(order);
                                }
                            } catch (Exception e) {
                                Toast.makeText(AdmOrderManagementActivity.this,
                                        "Error loading order", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(AdmOrderManagementActivity.this,
                                "Failed to load orders", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void inflateOrderCard(Order order) {
        LinearLayout cardView = (LinearLayout) layoutInflater
                .inflate(R.layout.item_order_card, ordersContainer, false);

        TextView tvOrderId = cardView.findViewById(R.id.tvOrderId);
        TextView tvCustomerName = cardView.findViewById(R.id.tvCustomerName);
        TextView tvItemsAndTotal = cardView.findViewById(R.id.tvItemsAndTotal);
        TextView tvStatus = cardView.findViewById(R.id.tvStatus);
        Button btnAssignRider = cardView.findViewById(R.id.btnAssignRider);

        tvOrderId.setText("Order #" + order.getOrderId());
        tvCustomerName.setText("Customer: " + order.getCustomerName());
        tvItemsAndTotal.setText("Items: " + order.getItemsAsString() + "  |  Total: " + order.getTotalFormatted());
        tvStatus.setText("Status: " + order.getStatus());

        btnAssignRider.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdmAssignRiderActivity.class);
            intent.putExtra("orderId", order.getOrderId());
            startActivity(intent);
        });

        ordersContainer.addView(cardView);
>>>>>>> cc33148f16efd7cd1a6422a65c9b53b87be2e710
    }
}