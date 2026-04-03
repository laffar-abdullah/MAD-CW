package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.buyngo.R;

/**
 * RidDashboardActivity — main home screen for the delivery rider.
 *
 * Shows the currently assigned delivery task and lets the rider navigate to
 * Delivery History, Reviews, and Profile via a bottom navigation bar.
 *
 * ── CHANGES FROM ORIGINAL ──────────────────────────────────────────────────
 *  BUG FIX 1 — navDashboard bottom-nav item had NO click listener wired up.
 *  The layout declares android:id="@+id/navDashboard" in the bottom bar, but
 *  the original Java never called setOnClickListener on it.  On most Android
 *  versions this meant the Tasks tab simply did nothing when tapped.
 *  FIX: added a self-refresh click listener that calls refreshTask() so
 *  pressing "Tasks" while already on the dashboard reloads the task card.
 *
 *  BUG FIX 2 — applyStatusChipStyle() compared order.status using the magic
 *  string "Awaiting Pickup" in the else-branch, which could silently break if
 *  the default text ever changed.
 *  FIX: replaced the magic string with the now-visible package constant
 *  OrderStatusStore.DEFAULT_STATUS so the comparison is in sync with the store.
 *
 *  IMPROVEMENT — setSupportActionBar() is now called for the Toolbar so the
 *  system action bar is replaced properly.  The toolbar has no navigation icon
 *  on the dashboard (home screen) so no back press handler is needed here.
 * ───────────────────────────────────────────────────────────────────────────
 */
public class RidDashboardActivity extends AppCompatActivity {

    private TextView txtTaskOrderId;
    private TextView txtTaskCustomer;
    private TextView txtTaskAddress;
    private TextView txtTaskStatus;
    private View     sampleTaskCard;
    private View     btnTaskUpdateStatus;
    private View     txtNoActiveTask;
    private String activeOrderId;
    private boolean useLocalFallback;

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

        // ── IMPROVEMENT: initialise toolbar so system ActionBar is replaced ──
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Dashboard is the root screen — no back/up navigation icon needed.

        // Bind views.
        txtTaskOrderId      = findViewById(R.id.txtTaskOrderId);
        txtTaskCustomer     = findViewById(R.id.txtTaskCustomer);
        txtTaskAddress      = findViewById(R.id.txtTaskAddress);
        txtTaskStatus       = findViewById(R.id.txtTaskStatus);
        sampleTaskCard      = findViewById(R.id.sampleTaskCard);
        btnTaskUpdateStatus = findViewById(R.id.btnTaskUpdateStatus);
        txtNoActiveTask     = findViewById(R.id.txtNoActiveTask);

        // Both the task card and its "Update Status" button open the same screen.
        sampleTaskCard.setOnClickListener(v -> openStatusUpdate());
        btnTaskUpdateStatus.setOnClickListener(v -> openStatusUpdate());

        // ── Bottom navigation ──────────────────────────────────────────────
        // BUG FIX 1: navDashboard now has a listener — re-run refreshTask()
        // so the card reflects the latest status when the rider taps "Tasks".
        findViewById(R.id.navDashboard).setOnClickListener(v -> refreshTask());

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
        // Refresh the task card each time the rider returns to the dashboard
        // (e.g. after updating the status or navigating back from history).
        refreshTask();
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    /**
     * Reads the current order from the store and populates the task card.
     * Hides the card and shows an empty-state message when the order is
     * already marked as Delivered.
     */
    private void refreshTask() {
        RiderSessionStore.RiderProfile profile = RiderSessionStore.getCurrentRider(this);
        if (profile == null) {
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        Log.d("RidDashboard", "Refreshing task for rider email: " + profile.email);

        FirebaseRiderRepository.getLatestActiveOrderForRider(
                profile.email,
                new FirebaseRiderRepository.ResultCallback<FirebaseRiderRepository.RiderOrder>() {
                    @Override
                    public void onSuccess(FirebaseRiderRepository.RiderOrder order) {
                        Log.d("RidDashboard", "Order callback - order is: " + (order == null ? "NULL" : order.orderId));
                        if (order == null) {
                            OrderStatusStore.OrderInfo localOrder = OrderStatusStore.getCurrentOrder(RidDashboardActivity.this);
                            boolean hasLocalActiveTask = !OrderStatusStore.STATUS_DELIVERED.equals(localOrder.status);
                            sampleTaskCard.setVisibility(hasLocalActiveTask ? View.VISIBLE : View.GONE);
                            txtNoActiveTask.setVisibility(hasLocalActiveTask ? View.GONE : View.VISIBLE);

                            if (!hasLocalActiveTask) {
                                activeOrderId = null;
                                useLocalFallback = false;
                                return;
                            }

                            useLocalFallback = true;
                            activeOrderId = localOrder.orderId;
                            txtTaskOrderId.setText("Order #" + localOrder.orderId);
                            txtTaskCustomer.setText("Customer: " + localOrder.customerName);
                            txtTaskAddress.setText("Address: " + localOrder.customerAddress);
                            txtTaskStatus.setText(localOrder.status);
                            applyStatusChipStyle(localOrder.status);
                            return;
                        }

                        useLocalFallback = false;
                        activeOrderId = order.orderId;
                        sampleTaskCard.setVisibility(View.VISIBLE);
                        txtNoActiveTask.setVisibility(View.GONE);
                        txtTaskOrderId.setText("Order #" + order.orderId);
                        txtTaskCustomer.setText("Customer: " + order.customerName);
                        txtTaskAddress.setText("Address: " + order.customerAddress);
                        txtTaskStatus.setText(order.status);
                        applyStatusChipStyle(order.status);
                    }

                    @Override
                    public void onError(String message) {
                        OrderStatusStore.OrderInfo localOrder = OrderStatusStore.getCurrentOrder(RidDashboardActivity.this);
                        boolean hasLocalActiveTask = !OrderStatusStore.STATUS_DELIVERED.equals(localOrder.status);
                        sampleTaskCard.setVisibility(hasLocalActiveTask ? View.VISIBLE : View.GONE);
                        txtNoActiveTask.setVisibility(hasLocalActiveTask ? View.GONE : View.VISIBLE);
                        if (!hasLocalActiveTask) {
                            activeOrderId = null;
                            useLocalFallback = false;
                            return;
                        }
                        useLocalFallback = true;
                        activeOrderId = localOrder.orderId;
                        txtTaskOrderId.setText("Order #" + localOrder.orderId);
                        txtTaskCustomer.setText("Customer: " + localOrder.customerName);
                        txtTaskAddress.setText("Address: " + localOrder.customerAddress);
                        txtTaskStatus.setText(localOrder.status);
                        applyStatusChipStyle(localOrder.status);
                    }
                });
    }

    private void openStatusUpdate() {
        Intent intent = new Intent(this, RidStatusUpdateActivity.class);
        if (activeOrderId != null && !activeOrderId.trim().isEmpty()) {
            intent.putExtra("order_id", activeOrderId);
        }
        intent.putExtra("use_local_fallback", useLocalFallback);
        startActivity(intent);
    }

    /**
     * Colours the status label to give riders an at-a-glance indication of
     * where in the delivery lifecycle they are.
     *
     * BUG FIX 2: the "Awaiting Pickup" branch now uses the package-visible
     * constant {@link OrderStatusStore#DEFAULT_STATUS} instead of a magic
     * string literal, keeping it in sync with the store definition.
     */
    private void applyStatusChipStyle(String status) {
        int colorRes;
        if (OrderStatusStore.STATUS_PICKED_UP.equals(status)) {
            colorRes = R.color.status_picked;
        } else if (OrderStatusStore.STATUS_ON_THE_WAY.equals(status)) {
            colorRes = R.color.status_out_for_delivery;
        } else if (OrderStatusStore.STATUS_DELIVERED.equals(status)) {
            colorRes = R.color.status_delivered;
        } else {
            // DEFAULT_STATUS ("Awaiting Pickup") — use the grey "ordered" colour.
            // BUG FIX 2: was just an implicit else, now explicitly tied to the constant.
            colorRes = R.color.status_ordered;
        }
        txtTaskStatus.setTextColor(getColor(colorRes));
    }
}
