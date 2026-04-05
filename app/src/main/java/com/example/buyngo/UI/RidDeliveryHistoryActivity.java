package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.buyngo.R;

import java.text.DateFormat;
import java.util.List;

public class RidDeliveryHistoryActivity extends AppCompatActivity {

    // Container that receives the dynamically inflated history cards.
    private LinearLayout historyContainer;

    // Empty-state label shown when the rider has no completed deliveries yet.
    // BUG FIX: this view must live OUTSIDE historyContainer in the layout so
    // removeAllViews() does not detach it.  See rid_delivery_history.xml.
    private TextView txtEmptyHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if rider is logged in
        if (!RiderSessionStore.isLoggedIn(this)) {
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.rid_delivery_history);

        // Bind views and setup toolbar
        historyContainer = findViewById(R.id.historyContainer);
        txtEmptyHistory  = findViewById(R.id.txtEmptyHistory);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        findViewById(R.id.navDashboard).setOnClickListener(v ->
                startActivity(new Intent(this, RidDashboardActivity.class)));

        // Re-render list on history tab tap
        findViewById(R.id.navHistory).setOnClickListener(v -> renderHistory());

        findViewById(R.id.navReviews).setOnClickListener(v ->
                startActivity(new Intent(this, RidReviewsActivity.class)));

        findViewById(R.id.navProfile).setOnClickListener(v ->
                startActivity(new Intent(this, RidProfileActivity.class)));

        renderHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-render so a delivery just completed appears without a manual
        // refresh (e.g. rider presses back from RidStatusUpdateActivity).
        renderHistory();
    }


    /**
     * Display all delivered orders for the current rider
     */
    private void renderHistory() {
        // Get current logged-in rider information
        RiderSessionStore.RiderProfile profile = RiderSessionStore.getCurrentRider(this);
        if (profile == null) {
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        // Fetch delivered orders from Firebase database
        FirebaseRiderRepository.getDeliveredOrdersForRider(
                profile.email,
                new FirebaseRiderRepository.ResultCallback<List<FirebaseRiderRepository.RiderOrder>>() {
                    @Override
                    public void onSuccess(List<FirebaseRiderRepository.RiderOrder> records) {
                        // Update UI on main thread
                        runOnUiThread(() -> {
                            // If no orders found, show empty state message
                            if (records == null || records.isEmpty()) {
                                txtEmptyHistory.setVisibility(View.VISIBLE);
                                historyContainer.removeAllViews();
                                return;
                            }

                            android.util.Log.d("RidDeliveryHistory", "══════════════════════════════════════════════════════════");
                            android.util.Log.d("RidDeliveryHistory", "Found " + records.size() + " delivered orders, rendering all...");
                            txtEmptyHistory.setVisibility(View.GONE);
                            historyContainer.removeAllViews();

                            // Create card for each delivered order
                            LayoutInflater inflater = LayoutInflater.from(RidDeliveryHistoryActivity.this);
                            DateFormat dateFormat = android.text.format.DateFormat
                                    .getMediumDateFormat(RidDeliveryHistoryActivity.this);

                            int cardCount = 0;
                            for (FirebaseRiderRepository.RiderOrder record : records) {
                                try {
                                    android.util.Log.d("RidDeliveryHistory", "  [" + (cardCount + 1) + "/" + records.size() + "] Inflating card for order: " + record.orderId);
                                    
                                    View card = inflater.inflate(
                                            R.layout.item_delivery_history, historyContainer, false);

                                    // Bind card views
                                    TextView txtOrderId = card.findViewById(R.id.txtHistoryOrderId);
                                    TextView txtCustomer = card.findViewById(R.id.txtHistoryCustomer);
                                    TextView txtAddress = card.findViewById(R.id.txtHistoryAddress);
                                    TextView txtDate = card.findViewById(R.id.txtHistoryDate);

                                    if (txtOrderId == null) {
                                        android.util.Log.e("RidDeliveryHistory", "ERROR: txtHistoryOrderId not found in layout!");
                                        continue;
                                    }
                                    if (txtCustomer == null) {
                                        android.util.Log.e("RidDeliveryHistory", "ERROR: txtHistoryCustomer not found in layout!");
                                        continue;
                                    }
                                    if (txtAddress == null) {
                                        android.util.Log.e("RidDeliveryHistory", "ERROR: txtHistoryAddress not found in layout!");
                                        continue;
                                    }
                                    if (txtDate == null) {
                                        android.util.Log.e("RidDeliveryHistory", "ERROR: txtHistoryDate not found in layout!");
                                        continue;
                                    }

                                    txtOrderId.setText("Order #" + record.orderId);
                                    txtCustomer.setText("Customer: " + record.customerName);
                                    
                                    // Display address with phone number combined
                                    StringBuilder addressInfo = new StringBuilder();
                                    if (record.customerAddress != null && !record.customerAddress.isEmpty()) {
                                        addressInfo.append("Address: ").append(record.customerAddress);
                                    } else {
                                        addressInfo.append("Address: Not provided");
                                    }

                                    // Add phone number if available
                                    if (record.customerPhone != null && !record.customerPhone.isEmpty()) {
                                        addressInfo.append("\nPhone: ").append(record.customerPhone);
                                    }

                                    String itemsDisplay = formatItemsList(record.itemsList, record.items);
                                    if (!itemsDisplay.isEmpty()) {
                                        addressInfo.append("\nItems: ").append(itemsDisplay);
                                    }

                                    // Add order total
                                    if (record.totalAmount > 0) {
                                        addressInfo.append("\nTotal: ").append(String.format("Rs. %.2f", record.totalAmount));
                                    }

                                    txtAddress.setText(addressInfo.toString());
                                    txtDate.setText("Date: " + dateFormat.format(record.deliveredAt));

                                    // CRITICAL: Explicitly set layout params to ensure card is displayed
                                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT);
                                    card.setLayoutParams(layoutParams);
                                    
                                    historyContainer.addView(card);
                                    cardCount++;
                                    android.util.Log.d("RidDeliveryHistory", "    ✓ Card added successfully. Total in container: " + historyContainer.getChildCount());
                                } catch (Exception e) {
                                    android.util.Log.e("RidDeliveryHistory", "Error inflating card for order " + record.orderId, e);
                                    e.printStackTrace();
                                }
                            }
                            android.util.Log.d("RidDeliveryHistory", "══════════════════════════════════════════════════════════");
                            android.util.Log.d("RidDeliveryHistory", "✓ renderHistory COMPLETE - Total cards added: " + cardCount + " | Container child count: " + historyContainer.getChildCount());
                            android.util.Log.d("RidDeliveryHistory", "══════════════════════════════════════════════════════════");
                        });
                    }

                    @Override
                    public void onError(String message) {
                        // Show empty state on error
                        txtEmptyHistory.setVisibility(View.VISIBLE);
                        historyContainer.removeAllViews();
                    }
                });
    }

    // Helper method to format items list
    private String formatItemsList(java.util.List<Object> itemsList, java.util.Map<String, Integer> itemsMap) {
        StringBuilder sb = new StringBuilder();

        if (itemsList != null && !itemsList.isEmpty()) {
            // Format from itemsList
            for (Object item : itemsList) {
                if (item instanceof java.util.Map) {
                    java.util.Map<String, Object> itemMap = (java.util.Map<String, Object>) item;
                    String name = (String) itemMap.get("name");
                    Object qtyObj = itemMap.get("quantity");
                    int qty = (qtyObj instanceof Integer) ? (Integer) qtyObj : ((Number) qtyObj).intValue();
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(qty).append("x ").append(name);
                }
            }
        } else if (itemsMap != null && !itemsMap.isEmpty()) {
            // Fallback to items map
            for (java.util.Map.Entry<String, Integer> entry : itemsMap.entrySet()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(entry.getValue()).append("x ").append(entry.getKey());
            }
        }
        return sb.toString();
    }
}
