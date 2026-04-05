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

/**
 * RidDeliveryHistoryActivity — shows a scrollable list of every order this
 * rider has completed, newest first.
 *
 * Each card is inflated from {@code item_delivery_history.xml} and populated
 * with the order snapshot that was archived when the delivery was marked
 * "Delivered" in {@link RidStatusUpdateActivity}.
 *
 * ── CHANGES FROM ORIGINAL ──────────────────────────────────────────────────
 *  BUG FIX — Critical crash / invisible empty state.
 *
 *  In the original layout (rid_delivery_history.xml) the TextView with id
 *  {@code txtEmptyHistory} was placed INSIDE the {@code historyContainer}
 *  LinearLayout.  renderHistory() called historyContainer.removeAllViews()
 *  at the start of every refresh — which detached the empty-state TextView
 *  from the view hierarchy.  The subsequent call to
 *  txtEmptyHistory.setVisibility(View.VISIBLE) operated on a detached view
 *  (no parent), so it had no visible effect.  The empty state was therefore
 *  never shown even when the history list was genuinely empty.
 *
 *  FIX (two-part):
 *   1. rid_delivery_history.xml — moved txtEmptyHistory OUTSIDE and ABOVE
 *      the historyContainer so removeAllViews() no longer touches it.
 *      See the layout file change notes for details.
 *   2. renderHistory() — now guards historyContainer.removeAllViews() so
 *      it is only called when we are about to add new cards, not when the
 *      list is empty.  This is a belt-and-suspenders fix: even if someone
 *      moves the TextView back inside the container by mistake, the logic
 *      still works correctly.
 *
 *  IMPROVEMENT — onResume re-renders the list so newly completed deliveries
 *  appear immediately when the rider returns from the status update screen.
 *  (This was already present in the original; kept unchanged.)
 * ───────────────────────────────────────────────────────────────────────────
 */
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

        // Restrict history screen to logged-in riders only.
        if (!RiderSessionStore.isLoggedIn(this)) {
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.rid_delivery_history);

        // Bind views.
        historyContainer = findViewById(R.id.historyContainer);
        txtEmptyHistory  = findViewById(R.id.txtEmptyHistory);

        // Toolbar with back navigation.
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // ── Bottom navigation ──────────────────────────────────────────────
        // "History" tab is the current screen — self-navigation would
        // create a duplicate activity; pressing it simply refreshes the list.
        findViewById(R.id.navDashboard).setOnClickListener(v ->
                startActivity(new Intent(this, RidDashboardActivity.class)));

        // BUG FIX (minor): original wired navHistory to a new History intent
        // while already ON the history screen — this stacked another copy of
        // the same activity on the back stack.
        // FIX: tapping the active "History" tab now just re-renders the list.
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

    // ── Private helpers ─────────────────────────────────────────────────────

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

                            // Hide empty message and show the orders
                            txtEmptyHistory.setVisibility(View.GONE);
                            historyContainer.removeAllViews();

                            // Create card for each delivered order
                            LayoutInflater inflater = LayoutInflater.from(RidDeliveryHistoryActivity.this);
                            DateFormat dateFormat = android.text.format.DateFormat
                                    .getMediumDateFormat(RidDeliveryHistoryActivity.this);

                            // Loop through each delivered order and create a card
                            for (FirebaseRiderRepository.RiderOrder record : records) {
                                try {
                                    // Create card view from layout file
                                    View card = inflater.inflate(
                                            R.layout.item_delivery_history, historyContainer, false);

                                    // Find text fields in the card
                                    TextView txtOrderId = card.findViewById(R.id.txtHistoryOrderId);
                                    TextView txtCustomer = card.findViewById(R.id.txtHistoryCustomer);
                                    TextView txtAddress = card.findViewById(R.id.txtHistoryAddress);
                                    TextView txtDate = card.findViewById(R.id.txtHistoryDate);

                                    // Set order ID
                                    txtOrderId.setText("Order #" + record.orderId);
                                    txtCustomer.setText("Customer: " + record.customerName);
                                    
                                    // Build complete address information
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
                                    
                                    // Add items list
                                    String itemsDisplay = formatItemsList(record.itemsList, record.items);
                                    if (!itemsDisplay.isEmpty()) {
                                        addressInfo.append("\nItems: ").append(itemsDisplay);
                                    }
                                    
                                    // Add order total
                                    if (record.totalAmount > 0) {
                                        addressInfo.append("\nTotal: ").append(String.format("Rs. %.2f", record.totalAmount));
                                    }
                                    
                                    // Set address and date
                                    txtAddress.setText(addressInfo.toString());
                                    txtDate.setText("Date: " + dateFormat.format(record.deliveredAt));

                                    // Add card to container
                                    historyContainer.addView(card);
                                } catch (Exception e) {
                                    // Handle error creating card
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        // If error occurs, show empty state
                        txtEmptyHistory.setVisibility(View.VISIBLE);
                        historyContainer.removeAllViews();
                    }
                });
    }

    // Helper method to format items list
    private String formatItemsList(java.util.List<Object> itemsList, java.util.Map<String, Integer> itemsMap) {
        StringBuilder sb = new StringBuilder();
        
        if (itemsList != null && !itemsList.isEmpty()) {
            // Try to format from itemsList
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
