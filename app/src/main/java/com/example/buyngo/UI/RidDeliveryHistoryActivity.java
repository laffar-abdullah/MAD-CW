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
     * Clears any previous cards and inflates one card per completed delivery,
     * ordered newest-first.
     *
     * BUG FIX: historyContainer.removeAllViews() is now called ONLY when
     * records exist, after we have confirmed txtEmptyHistory should be hidden.
     * When the list is empty we update visibility without touching the
     * container, so the empty-state TextView (which lives outside the
     * container in the fixed layout) is always reachable.
     */
    private void renderHistory() {
        List<OrderStatusStore.DeliveryRecord> records =
                OrderStatusStore.getDeliveryHistory(this);

        if (records.isEmpty()) {
            // BUG FIX: txtEmptyHistory is now a sibling of historyContainer
            // in the layout, so removeAllViews() can never detach it.
            // Simply make it visible — no need to touch historyContainer.
            txtEmptyHistory.setVisibility(View.VISIBLE);
            historyContainer.removeAllViews();   // clear any stale cards
            return;
        }

        // We have records — hide the empty state and rebuild the card list.
        txtEmptyHistory.setVisibility(View.GONE);
        historyContainer.removeAllViews();

        LayoutInflater inflater   = LayoutInflater.from(this);
        DateFormat     dateFormat = android.text.format.DateFormat.getMediumDateFormat(this);

        // Iterate in reverse so the newest delivery appears at the top.
        for (int i = records.size() - 1; i >= 0; i--) {
            OrderStatusStore.DeliveryRecord record = records.get(i);

            View card = inflater.inflate(
                    R.layout.item_delivery_history, historyContainer, false);

            TextView txtOrderId  = card.findViewById(R.id.txtHistoryOrderId);
            TextView txtCustomer = card.findViewById(R.id.txtHistoryCustomer);
            TextView txtAddress  = card.findViewById(R.id.txtHistoryAddress);
            TextView txtDate     = card.findViewById(R.id.txtHistoryDate);

            txtOrderId.setText("Order #"     + record.orderId);
            txtCustomer.setText("Customer: " + record.customerName);
            txtAddress.setText("Address: "   + record.customerAddress);
            txtDate.setText("Date: "         + dateFormat.format(record.deliveredAt));

            historyContainer.addView(card);
        }
    }
}
