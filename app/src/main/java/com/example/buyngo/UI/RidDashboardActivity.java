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
import com.example.buyngo.Utils.FirebaseOrderCleanup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * RidDashboardActivity — main home screen for the delivery rider.
 *
 * Shows all assigned delivery tasks and lets the rider navigate to
 * Delivery History, Reviews, and Profile via a bottom navigation bar.
 */
public class RidDashboardActivity extends AppCompatActivity {

    private LinearLayout ordersContainer;
    private TextView txtNoActiveTask;
    private FirebaseDatabase firebaseDatabase;
    private static final String TAG = "RidDashboard";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Rider screens are protected — redirect to login when session is missing.
        if (!RiderSessionStore.isLoggedIn(this)) {
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.rid_dashboard);

        firebaseDatabase = FirebaseDatabase.getInstance("https://buyngo-5b43e-default-rtdb.firebaseio.com/");

        // ── IMPROVEMENT: initialise toolbar so system ActionBar is replaced ──
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Dashboard is the root screen — no back/up navigation icon needed.

        // Bind views.
        ordersContainer = findViewById(R.id.ordersContainer);
        if (ordersContainer == null) {
            ordersContainer = new LinearLayout(this);
            ordersContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            ordersContainer.setOrientation(LinearLayout.VERTICAL);
        }

        txtNoActiveTask = findViewById(R.id.txtNoActiveTask);
        if (txtNoActiveTask == null) {
            txtNoActiveTask = new TextView(this);
            txtNoActiveTask.setText("No orders assigned");
            txtNoActiveTask.setTextSize(16);
            txtNoActiveTask.setPadding(16, 16, 16, 16);
        }

        // ── Bottom navigation ──────────────────────────────────────────────
        findViewById(R.id.navDashboard).setOnClickListener(v -> loadRiderOrders());

        findViewById(R.id.navHistory).setOnClickListener(v ->
                startActivity(new Intent(this, RidDeliveryHistoryActivity.class)));

        findViewById(R.id.navReviews).setOnClickListener(v ->
                startActivity(new Intent(this, RidReviewsActivity.class)));

        findViewById(R.id.navProfile).setOnClickListener(v ->
                startActivity(new Intent(this, RidProfileActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure all orders have proper itemsList initialized
        FirebaseOrderCleanup.ensureOrdersHaveItemsList();
        // Migrate old "Confirmed" orders that are assigned to riders to "Awaiting Pickup"
        FirebaseOrderCleanup.migrateConfirmedOrdersToAwaitingPickup();
        // Refresh the orders list each time the rider returns to the dashboard
        loadRiderOrders();
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    /**
     * Loads all orders assigned to the current rider from Firebase
     */
    private void loadRiderOrders() {
        RiderSessionStore.RiderProfile profile = RiderSessionStore.getCurrentRider(this);
        if (profile == null) {
            Toast.makeText(this, "Rider profile not found", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        Log.d(TAG, "Loading orders for rider email: " + profile.email);
        ordersContainer.removeAllViews();

        firebaseDatabase.getReference("orders")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        ordersContainer.removeAllViews();

                        if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                            txtNoActiveTask.setVisibility(View.VISIBLE);
                            ordersContainer.addView(txtNoActiveTask);
                            return;
                        }

                        Log.d(TAG, "Total orders in Firebase: " + snapshot.getChildrenCount());
                        Log.d(TAG, "Current rider email: " + profile.email);
                        int ordersFound = 0;

                        for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                            try {
                                Order order = orderSnapshot.getValue(Order.class);

                                if (order != null) {
                                    String assignedRiderEmail = order.getAssignedRiderEmail();
                                    Log.d(TAG, "Found order: " + order.getOrderId() + 
                                            ", assigned rider email: " + assignedRiderEmail + 
                                            ", status: " + order.getStatus());
                                    
                                    // Display order if it's assigned to current rider
                                    if (assignedRiderEmail != null && profile.email.equals(assignedRiderEmail)) {
                                        ordersFound++;
                                        Log.d(TAG, "✓ Displaying order: " + order.getOrderId());
                                        displayOrderCard(order);
                                    } else {
                                        Log.d(TAG, "⊘ Skipping order (different rider): " + order.getOrderId());
                                    }
                                } else {
                                    Log.w(TAG, "Order is null from snapshot");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error loading order", e);
                                Toast.makeText(RidDashboardActivity.this,
                                        "Error loading order: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        if (ordersFound == 0) {
                            txtNoActiveTask.setVisibility(View.VISIBLE);
                            ordersContainer.addView(txtNoActiveTask);
                            Log.d(TAG, "No orders found for rider: " + profile.email);
                        } else {
                            txtNoActiveTask.setVisibility(View.GONE);
                            Log.d(TAG, "✓ Found " + ordersFound + " orders for rider: " + profile.email);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "Failed to load orders: " + error.getMessage());
                        Toast.makeText(RidDashboardActivity.this,
                                "Failed to load orders: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Displays a single order card with status and action buttons
     */
    private void displayOrderCard(Order order) {
        // Enhanced logging to debug items display
        Log.d(TAG, "Displaying order: " + order.getOrderId() + 
              " | itemsList size: " + (order.getItemsList() != null ? order.getItemsList().size() : 0) +
              " | items map size: " + (order.getItems() != null ? order.getItems().size() : 0) +
              " | total: " + order.getTotalAmount());
        
        LinearLayout orderLayout = new LinearLayout(this);
        orderLayout.setOrientation(LinearLayout.VERTICAL);
        orderLayout.setPadding(16, 16, 16, 16);
        orderLayout.setBackgroundColor(getResources().getColor(R.color.white, null));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        orderLayout.setLayoutParams(params);

        // Order ID
        TextView tvOrderId = new TextView(this);
        tvOrderId.setText("Order #" + order.getOrderId());
        tvOrderId.setTextSize(18);
        tvOrderId.setTypeface(tvOrderId.getTypeface(), Typeface.BOLD);
        tvOrderId.setTextColor(getResources().getColor(R.color.primary_green, null));
        orderLayout.addView(tvOrderId);

        // Customer Name
        TextView tvCustomer = new TextView(this);
        tvCustomer.setText("Customer: " + order.getCustomerName());
        tvCustomer.setTextSize(14);
        tvCustomer.setTextColor(getResources().getColor(R.color.text_dark, null));
        tvCustomer.setPadding(0, 8, 0, 0);
        orderLayout.addView(tvCustomer);

        // Customer Address
        if (order.getCustomerName() != null) {
            TextView tvAddress = new TextView(this);
            tvAddress.setText("Address: " + (order.getCustomerName() != null ? "Pending" : "No address"));
            tvAddress.setTextSize(13);
            tvAddress.setTextColor(getResources().getColor(R.color.text_light, null));
            tvAddress.setPadding(0, 4, 0, 0);
            orderLayout.addView(tvAddress);
        }

        // Items
        String itemsDisplay = order.getItemsAsString();
        Log.d(TAG, "Order " + order.getOrderId() + " items display: " + itemsDisplay);
        
        TextView tvItems = new TextView(this);
        tvItems.setText("Items: " + itemsDisplay);
        tvItems.setTextSize(13);
        tvItems.setTextColor(getResources().getColor(R.color.text_light, null));
        tvItems.setPadding(0, 4, 0, 0);
        orderLayout.addView(tvItems);

        // Total Amount
        String totalDisplay = order.getTotalFormatted();
        Log.d(TAG, "Order " + order.getOrderId() + " total display: " + totalDisplay);
        
        TextView tvTotal = new TextView(this);
        tvTotal.setText("Total: " + totalDisplay);
        tvTotal.setTextSize(13);
        tvTotal.setTypeface(tvTotal.getTypeface(), Typeface.BOLD);
        tvTotal.setTextColor(getResources().getColor(R.color.primary_green, null));
        tvTotal.setPadding(0, 8, 0, 0);
        orderLayout.addView(tvTotal);

        // Status
        TextView tvStatus = new TextView(this);
        tvStatus.setText("Status: " + order.getStatus());
        tvStatus.setTextSize(14);
        tvStatus.setTypeface(tvStatus.getTypeface(), Typeface.BOLD);
        tvStatus.setTextColor(getStatusColor(order.getStatus()));
        tvStatus.setPadding(0, 8, 0, 0);
        orderLayout.addView(tvStatus);

        // Update Status Button (for pending/in-progress orders)
        if (!order.getStatus().equals("Delivered") && !order.getStatus().equals("Cancelled")) {
            Button btnUpdateStatus = new Button(this);
            btnUpdateStatus.setText("Update Status");
            btnUpdateStatus.setTextSize(14);
            btnUpdateStatus.setTextColor(getResources().getColor(R.color.white, null));
            btnUpdateStatus.setBackgroundColor(getResources().getColor(R.color.primary_green, null));
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            btnParams.setMargins(0, 12, 0, 0);
            btnUpdateStatus.setLayoutParams(btnParams);
            btnUpdateStatus.setOnClickListener(v -> {
                Intent intent = new Intent(RidDashboardActivity.this, RidStatusUpdateActivity.class);
                intent.putExtra("order_id", order.getOrderId());
                startActivity(intent);
            });
            orderLayout.addView(btnUpdateStatus);
        }

        // Separator line
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

    /**
     * Returns the appropriate color for a given order status
     */
    private int getStatusColor(String status) {
        if (status == null) {
            return getResources().getColor(R.color.status_ordered, null);
        }

        switch (status) {
            case "Confirmed":
            case "Awaiting Pickup":
                return getResources().getColor(R.color.status_ordered, null);
            case "Picked Up":
                return getResources().getColor(R.color.status_picked, null);
            case "On the Way":
                return getResources().getColor(R.color.status_out_for_delivery, null);
            case "Delivered":
                return getResources().getColor(R.color.status_delivered, null);
            default:
                return getResources().getColor(R.color.text_dark, null);
        }
    }
}
