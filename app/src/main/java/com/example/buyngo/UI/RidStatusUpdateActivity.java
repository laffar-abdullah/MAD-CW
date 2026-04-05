ackage com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.buyngo.R;


public class RidStatusUpdateActivity extends AppCompatActivity {

    private TextView txtOrderNumber;
    private TextView txtCurrentStatus;
    private View     btnPickedUp;
    private View     btnOutForDelivery;
    private View     btnDelivered;
    private String   currentOrderId;
    private boolean  forceLocalMode;
    private boolean  usingLocalFallback;

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

        currentOrderId = getIntent().getStringExtra("order_id");
        forceLocalMode = getIntent().getBooleanExtra("use_local_fallback", false);

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

    
    private void saveStatus(String status, boolean closeAfterUpdate) {
        if (usingLocalFallback || forceLocalMode || currentOrderId == null || currentOrderId.trim().isEmpty()) {
            boolean updated = OrderStatusStore.updateStatus(this, status);
            if (!updated) {
                Toast.makeText(this,
                        "Follow the order: Awaiting Pickup -> Picked Up -> On the Way -> Delivered",
                        Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(this, "Status updated: " + status, Toast.LENGTH_SHORT).show();
            refreshUi();
            if (closeAfterUpdate) {
                finish();
            }
            return;
        }

        // Get the current rider's email to save with the order update
        RiderSessionStore.RiderProfile profile = RiderSessionStore.getCurrentRider(this);
        String riderEmail = (profile != null) ? profile.email : null;

        FirebaseRiderRepository.updateOrderStatus(currentOrderId, status, riderEmail,
                new FirebaseRiderRepository.VoidCallback() {
                    @Override
                    public void onSuccess() {
                        OrderStatusStore.setStatus(RidStatusUpdateActivity.this, status);
                        Toast.makeText(RidStatusUpdateActivity.this,
                                "Status updated: " + status,
                                Toast.LENGTH_SHORT).show();
                        refreshUi();
                        if (closeAfterUpdate) {
                            finish();
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (message != null && message.toLowerCase().contains("not found")) {
                            boolean updated = OrderStatusStore.updateStatus(RidStatusUpdateActivity.this, status);
                            if (updated) {
                                Toast.makeText(RidStatusUpdateActivity.this,
                                        "Status updated: " + status,
                                        Toast.LENGTH_SHORT).show();
                                refreshUi();
                                if (closeAfterUpdate) {
                                    finish();
                                }
                                return;
                            }
                        }
                        Toast.makeText(RidStatusUpdateActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    
    private void refreshUi() {
        if (forceLocalMode) {
            loadFromLocalStore();
            return;
        }

        RiderSessionStore.RiderProfile profile = RiderSessionStore.getCurrentRider(this);
        if (profile == null) {
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        // Always show a usable local state immediately while Firebase loads.
        String requestedOrderId = currentOrderId;
        loadFromLocalStore();
        if (requestedOrderId != null && !requestedOrderId.trim().isEmpty()) {
            currentOrderId = requestedOrderId;
        }

        if (currentOrderId == null || currentOrderId.trim().isEmpty()) {
            FirebaseRiderRepository.getLatestActiveOrderForRider(
                    profile.email,
                    new FirebaseRiderRepository.ResultCallback<FirebaseRiderRepository.RiderOrder>() {
                        @Override
                        public void onSuccess(FirebaseRiderRepository.RiderOrder order) {
                            if (order == null) {
                                loadFromLocalStore();
                                return;
                            }
                            usingLocalFallback = false;
                            currentOrderId = order.orderId;
                            renderOrderState(order.orderId, order.status);
                        }

                        @Override
                        public void onError(String message) {
                                loadFromLocalStore();
                        }
                    });
            return;
        }

        FirebaseRiderRepository.getAssignedOrdersForRider(
                profile.email,
                new FirebaseRiderRepository.ResultCallback<java.util.List<FirebaseRiderRepository.RiderOrder>>() {
                    @Override
                    public void onSuccess(java.util.List<FirebaseRiderRepository.RiderOrder> orders) {
                        FirebaseRiderRepository.RiderOrder current = null;
                        for (FirebaseRiderRepository.RiderOrder order : orders) {
                            if (currentOrderId.equals(order.orderId)) {
                                current = order;
                                break;
                            }
                        }

                        if (current == null) {
                            loadFromLocalStore();
                            return;
                        }

                        usingLocalFallback = false;
                        renderOrderState(current.orderId, current.status);
                    }

                    @Override
                    public void onError(String message) {
                        loadFromLocalStore();
                    }
                });
    }

    private void setLoadingState() {
        txtOrderNumber.setText("Order: Loading...");
        txtCurrentStatus.setText("Current status: Loading...");
        setButtonState(btnPickedUp, false);
        setButtonState(btnOutForDelivery, false);
        setButtonState(btnDelivered, false);
    }

    private void loadFromLocalStore() {
        OrderStatusStore.OrderInfo order = OrderStatusStore.getCurrentOrder(this);
        usingLocalFallback = true;
        currentOrderId = order.orderId;
        renderOrderState(order.orderId, order.status);
    }

    private void renderOrderState(String orderId, String status) {
        txtOrderNumber.setText("Order: #" + orderId);
        txtCurrentStatus.setText("Current status: " + status);

        // Fallback: treat "Confirmed" as "Awaiting Pickup" for backward compatibility
        // (in case old orders haven't been migrated yet)
        String normalizedStatus = "Confirmed".equals(status) ? OrderStatusStore.DEFAULT_STATUS : status;

        // Only the next valid step stays clickable; past/future steps are dimmed.
        setButtonState(btnPickedUp, OrderStatusStore.DEFAULT_STATUS.equals(normalizedStatus));
        setButtonState(btnOutForDelivery, OrderStatusStore.STATUS_PICKED_UP.equals(normalizedStatus));
        setButtonState(btnDelivered, OrderStatusStore.STATUS_ON_THE_WAY.equals(normalizedStatus));
    }

    
    private void setButtonState(View button, boolean enabled) {
        button.setEnabled(enabled);
        // Full opacity when active; 45 % when not the valid next step.
        button.setAlpha(enabled ? 1f : 0.45f);
    }
}

