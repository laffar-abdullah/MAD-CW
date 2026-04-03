package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.buyngo.R;

/**
 * RidStatusUpdateActivity — lets the rider advance the delivery status
 * through its fixed lifecycle:
 *
 *   Awaiting Pickup → Picked Up → On the Way → Delivered
 *
 * Three buttons correspond to the three forward steps.  At any given moment
 * only the single valid next step is enabled; the other two are dimmed.
 * When "Delivered" is tapped the activity closes and returns the rider to
 * the Dashboard, which will show the "no active task" empty state.
 *
 * ── CHANGES FROM ORIGINAL ──────────────────────────────────────────────────
 *  BUG FIX 1 — The layout button android:id="@+id/btnOutForDelivery" has the
 *  label "Mark as Out for Delivery" but the status constant it was writing is
 *  STATUS_ON_THE_WAY ("On the Way").  The toast in OrderStatusStore.updateStatus
 *  then showed "Status updated: On the Way" while the button said "Out for
 *  Delivery" — confusing mismatch for the rider.
 *  FIX: kept STATUS_ON_THE_WAY as the stored value (it drives customer tracking
 *  colour coding and transition guards) but updated the button label in the
 *  layout (rid_status_update.xml) to "Mark as On the Way" so the UI and store
 *  agree.  See rid_status_update.xml change notes.
 *
 *  IMPROVEMENT — Added a Toolbar with a back / up button so the rider can
 *  return to the Dashboard without using the Android system back gesture.
 *  Previously there was no toolbar at all on this screen.
 *
 *  IMPROVEMENT — refreshUi() now also enables/disables the "Picked Up" button
 *  correctly when status is DEFAULT_STATUS ("Awaiting Pickup") using the
 *  package-visible constant instead of a magic string literal.
 * ───────────────────────────────────────────────────────────────────────────
 */
public class RidStatusUpdateActivity extends AppCompatActivity {

    private TextView txtOrderNumber;
    private TextView txtCurrentStatus;
    private View     btnPickedUp;
    private View     btnOutForDelivery;
    private View     btnDelivered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Prevent direct access when rider is not authenticated.
        if (!RiderSessionStore.isLoggedIn(this)) {
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.rid_status_update);

        // ── IMPROVEMENT: set up toolbar with back navigation ──
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Pressing the up arrow returns to whichever screen launched this
        // activity (Dashboard or the task card tap).
        toolbar.setNavigationOnClickListener(v -> finish());

        txtOrderNumber   = findViewById(R.id.txtOrderNumber);
        txtCurrentStatus = findViewById(R.id.txtCurrentStatus);
        btnPickedUp       = findViewById(R.id.btnPickedUp);
        btnOutForDelivery = findViewById(R.id.btnOutForDelivery);
        btnDelivered      = findViewById(R.id.btnDelivered);

        // Show the current order and enable only the valid next step.
        refreshUi();

        // Each button advances the status by exactly one step.
        // BUG FIX 1: all three buttons store the correct STATUS_* constant.
        // The "Out for Delivery" button stores STATUS_ON_THE_WAY which is
        // "On the Way" — matching what the layout label and toast will say.
        btnPickedUp.setOnClickListener(v ->
                saveStatus(OrderStatusStore.STATUS_PICKED_UP, false));

        btnOutForDelivery.setOnClickListener(v ->
                saveStatus(OrderStatusStore.STATUS_ON_THE_WAY, false));

        // Delivered closes the screen so the rider lands back on Dashboard.
        btnDelivered.setOnClickListener(v ->
                saveStatus(OrderStatusStore.STATUS_DELIVERED, true));
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    /**
     * Attempts to write {@code status} to the store.
     * Shows a toast if the transition is not allowed (e.g. rider tried to
     * skip a step).  Closes the activity when {@code closeAfterUpdate} is
     * true (used for the Delivered step).
     */
    private void saveStatus(String status, boolean closeAfterUpdate) {
        boolean updated = OrderStatusStore.updateStatus(this, status);
        if (!updated) {
            Toast.makeText(this,
                    "Follow the order: Awaiting Pickup → Picked Up → On the Way → Delivered",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "Status updated: " + status, Toast.LENGTH_SHORT).show();
        refreshUi();

        if (closeAfterUpdate) {
            // Return to Dashboard — it will show the empty-state on resume.
            finish();
        }
    }

    /**
     * Synchronises the order header and button states with the latest value
     * in the store.
     *
     * IMPROVEMENT: uses the package-visible DEFAULT_STATUS constant instead
     * of the magic string "Awaiting Pickup" so the check stays in sync if the
     * default text is ever changed in OrderStatusStore.
     */
    private void refreshUi() {
        OrderStatusStore.OrderInfo order = OrderStatusStore.getCurrentOrder(this);

        txtOrderNumber.setText("Order: #" + order.orderId);
        txtCurrentStatus.setText("Current status: " + order.status);

        // Only the single valid next step is active; others are dimmed.
        setButtonState(btnPickedUp,
                OrderStatusStore.DEFAULT_STATUS.equals(order.status));

        setButtonState(btnOutForDelivery,
                OrderStatusStore.STATUS_PICKED_UP.equals(order.status));

        setButtonState(btnDelivered,
                OrderStatusStore.STATUS_ON_THE_WAY.equals(order.status));
    }

    /**
     * Enables or disables a button and adjusts its visual opacity so the
     * rider can instantly see which step is available.
     */
    private void setButtonState(View button, boolean enabled) {
        button.setEnabled(enabled);
        // Full opacity when active; 45 % when not the valid next step.
        button.setAlpha(enabled ? 1f : 0.45f);
    }
}
