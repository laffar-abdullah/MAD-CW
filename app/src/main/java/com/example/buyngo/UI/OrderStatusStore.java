package com.example.buyngo.UI;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * OrderStatusStore — single source of truth for delivery order state.
 *
 * All order data (current status, customer info, delivery history) is stored
 * in SharedPreferences.  Every key is "scoped" to the currently logged-in
 * rider so two riders working on the same device never see each other's data.
 *
 * The un-scoped (global) keys act as a read-only fallback for the customer
 * tracking screen, which does not have a rider session.
 *
 * ── CHANGES FROM ORIGINAL ──────────────────────────────────────────────────
 *  BUG FIX — appendDeliveryHistory() was reading the GLOBAL (un-scoped) keys
 *  when building the history record snapshot.  Those keys always contained the
 *  hard-coded default values ("BNG-001 / Alice Johnson / 12 Main Street")
 *  because only the scoped keys were updated during a normal delivery flow.
 *  Result: every completed delivery appeared as the same default order in
 *  history, regardless of which order was actually delivered.
 *
 *  FIX: appendDeliveryHistory() now calls getCurrentOrder(context) — the
 *  same helper used by the dashboard and status update screens — which already
 *  knows how to prefer scoped keys over global fallbacks.  This ensures the
 *  history record always captures the real order that was just delivered.
 *
 *  CHANGE — STATUS_PICKED_UP, STATUS_ON_THE_WAY, STATUS_DELIVERED are now
 *  package-private (no modifier change needed, they already were), but
 *  DEFAULT_STATUS is also exposed as a package-private constant so that
 *  RidDashboardActivity can compare against it by name instead of relying
 *  on a magic string literal.
 * ───────────────────────────────────────────────────────────────────────────
 */
public final class OrderStatusStore {

    // SharedPreferences file shared by all order-related data.
    private static final String PREFS_NAME = "buyngo_order_status";

    // Base key names — these are used as-is for the global (customer-visible)
    // copy and as a prefix for the rider-scoped copies.
    private static final String KEY_ORDER_STATUS      = "order_status";
    private static final String KEY_ORDER_ID          = "order_id";
    private static final String KEY_CUSTOMER_NAME     = "customer_name";
    private static final String KEY_CUSTOMER_ADDRESS  = "customer_address";
    private static final String KEY_DELIVERY_HISTORY  = "delivery_history";

    // Delivery lifecycle status constants — used by rider UI and customer
    // tracking. Package-private so all classes in this package can reference
    // them without magic strings.
    static final String STATUS_PICKED_UP   = "Picked Up";
    static final String STATUS_ON_THE_WAY  = "On the Way";
    static final String STATUS_DELIVERED   = "Delivered Successfully";

    // ── CHANGE: DEFAULT_STATUS is now package-private so RidDashboardActivity
    // can compare the status chip color correctly without a magic string.
    static final String DEFAULT_STATUS = "Awaiting Pickup";

    // Seed values written during initializeDefaultsIfMissing().
    private static final String DEFAULT_ORDER_ID         = "BNG-001";
    private static final String DEFAULT_CUSTOMER_NAME    = "Alice Johnson";
    private static final String DEFAULT_CUSTOMER_ADDRESS = "12 Main Street, Springfield";

    // Utility class — no instances.
    private OrderStatusStore() { }

    // ── Status write / read ─────────────────────────────────────────────────

    /**
     * Overwrites both the scoped AND global status so the customer tracking
     * screen always reflects the latest rider update.
     */
    static void setStatus(Context context, String status) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                // Global key — read by CusTrackingActivity.
                .putString(KEY_ORDER_STATUS, status)
                // Scoped key — read by all Rid* screens.
                .putString(getScopedKey(context, KEY_ORDER_STATUS), status)
                .apply();
    }

    /**
     * Validates the status transition, applies it if legal, and archives the
     * order when it reaches Delivered.
     *
     * @return {@code true} when the status was updated, {@code false} when the
     *         requested transition is not allowed.
     */
    static boolean updateStatus(Context context, String newStatus) {
        String currentStatus = getStatus(context);
        if (!isValidTransition(currentStatus, newStatus)) {
            return false;
        }

        setStatus(context, newStatus);

        // Archive the order into delivery history the first time it is marked
        // as Delivered (guard prevents double-archiving on idempotent calls).
        if (STATUS_DELIVERED.equals(newStatus) && !STATUS_DELIVERED.equals(currentStatus)) {
            appendDeliveryHistory(context);
        }
        return true;
    }

    /**
     * Returns the delivery status for the current rider.
     * Scoped key is preferred; falls back to the global key so the very first
     * login (before initialization) still returns a sensible value.
     */
    static String getStatus(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String scoped = prefs.getString(getScopedKey(context, KEY_ORDER_STATUS), null);
        if (scoped != null) {
            return scoped;
        }
        return prefs.getString(KEY_ORDER_STATUS, DEFAULT_STATUS);
    }

    // ── Order snapshot ──────────────────────────────────────────────────────

    /**
     * Returns the full order snapshot for the current rider.
     * Used by RidDashboardActivity, RidStatusUpdateActivity, and
     * RidProfileActivity (for delivery count).
     */
    static OrderInfo getCurrentOrder(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Each field prefers the rider-scoped key and falls back to the global.
        String orderId = prefs.getString(
                getScopedKey(context, KEY_ORDER_ID),
                prefs.getString(KEY_ORDER_ID, DEFAULT_ORDER_ID));

        String customerName = prefs.getString(
                getScopedKey(context, KEY_CUSTOMER_NAME),
                prefs.getString(KEY_CUSTOMER_NAME, DEFAULT_CUSTOMER_NAME));

        String customerAddress = prefs.getString(
                getScopedKey(context, KEY_CUSTOMER_ADDRESS),
                prefs.getString(KEY_CUSTOMER_ADDRESS, DEFAULT_CUSTOMER_ADDRESS));

        String status = getStatus(context);

        return new OrderInfo(orderId, customerName, customerAddress, status);
    }

    // ── Delivery history ────────────────────────────────────────────────────

    /**
     * Returns every completed delivery archived for the current rider,
     * oldest first.  The list is empty when no deliveries have been made yet.
     */
    static List<DeliveryRecord> getDeliveryHistory(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(getScopedHistoryKey(context), "[]");

        List<DeliveryRecord> records = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                records.add(new DeliveryRecord(
                        obj.optString("orderId",         DEFAULT_ORDER_ID),
                        obj.optString("customerName",    DEFAULT_CUSTOMER_NAME),
                        obj.optString("customerAddress", DEFAULT_CUSTOMER_ADDRESS),
                        obj.optLong("deliveredAt",       System.currentTimeMillis())));
            }
        } catch (JSONException ignored) {
            // Ignore and return whatever was parsed — keeps app usable even
            // if old/corrupted data is present in storage.
        }
        return records;
    }

    // ── Seed / initialisation ───────────────────────────────────────────────

    /**
     * Writes default order values the first time a rider session starts so the
     * dashboard and status update screens always have data to display.
     * Does NOT overwrite keys that already exist.
     */
    static void initializeDefaultsIfMissing(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // ── Scoped keys (rider-specific) ──
        conditionalPut(editor, prefs, getScopedKey(context, KEY_ORDER_ID),         DEFAULT_ORDER_ID);
        conditionalPut(editor, prefs, getScopedKey(context, KEY_CUSTOMER_NAME),    DEFAULT_CUSTOMER_NAME);
        conditionalPut(editor, prefs, getScopedKey(context, KEY_CUSTOMER_ADDRESS), DEFAULT_CUSTOMER_ADDRESS);
        conditionalPut(editor, prefs, getScopedKey(context, KEY_ORDER_STATUS),     DEFAULT_STATUS);

        // ── Global keys (customer tracking fallback) ──
        conditionalPut(editor, prefs, KEY_ORDER_ID,         DEFAULT_ORDER_ID);
        conditionalPut(editor, prefs, KEY_CUSTOMER_NAME,    DEFAULT_CUSTOMER_NAME);
        conditionalPut(editor, prefs, KEY_CUSTOMER_ADDRESS, DEFAULT_CUSTOMER_ADDRESS);
        conditionalPut(editor, prefs, KEY_ORDER_STATUS,     DEFAULT_STATUS);

        editor.apply();
    }

    /** Writes {@code value} only when {@code key} is not already present. */
    private static void conditionalPut(
            SharedPreferences.Editor editor,
            SharedPreferences prefs,
            String key,
            String value) {
        if (!prefs.contains(key)) {
            editor.putString(key, value);
        }
    }

    // ── Status transition validation ────────────────────────────────────────

    /**
     * Defines the allowed forward-only delivery lifecycle:
     *   Awaiting Pickup → Picked Up → On the Way → Delivered
     *
     * Idempotent writes (same → same) are also accepted so the UI can safely
     * re-save the current status without triggering an error.
     */
    private static boolean isValidTransition(String current, String next) {
        if (current.equals(next)) {
            return true;                    // Idempotent — allow
        }
        if (DEFAULT_STATUS.equals(current)) {
            return STATUS_PICKED_UP.equals(next);
        }
        if (STATUS_PICKED_UP.equals(current)) {
            return STATUS_ON_THE_WAY.equals(next);
        }
        if (STATUS_ON_THE_WAY.equals(current)) {
            return STATUS_DELIVERED.equals(next);
        }
        return false;
    }

    // ── History archival ────────────────────────────────────────────────────

    /**
     * Appends a snapshot of the just-delivered order to the rider-scoped
     * history list.
     *
     * ── BUG FIX ──────────────────────────────────────────────────────────
     *  ORIGINAL: read from the global KEY_ORDER_ID / KEY_CUSTOMER_NAME /
     *  KEY_CUSTOMER_ADDRESS keys.  These keys always held the hard-coded seed
     *  values ("BNG-001", "Alice Johnson", "12 Main Street") because the
     *  actual delivery data was only ever written to the SCOPED keys.  Every
     *  history record therefore showed the same default order.
     *
     *  FIX: call getCurrentOrder(context) instead, which already knows how to
     *  prefer scoped keys and fall back to global ones.  This guarantees the
     *  archived snapshot always reflects the real order the rider just
     *  completed, not the seed defaults.
     * ─────────────────────────────────────────────────────────────────────
     */
    private static void appendDeliveryHistory(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // ── FIX: use getCurrentOrder so we read the scoped keys ──
        OrderInfo order = getCurrentOrder(context);

        String historyKey = getScopedHistoryKey(context);
        String raw = prefs.getString(historyKey, "[]");

        try {
            JSONArray arr = new JSONArray(raw);
            JSONObject item = new JSONObject();
            item.put("orderId",         order.orderId);
            item.put("customerName",    order.customerName);
            item.put("customerAddress", order.customerAddress);
            item.put("deliveredAt",     System.currentTimeMillis());
            arr.put(item);
            prefs.edit().putString(historyKey, arr.toString()).apply();
        } catch (JSONException ignored) {
            // Safe to swallow — history is cosmetic and a single failed write
            // should not crash the app or block the delivery flow.
        }
    }

    // ── Key scoping helpers ─────────────────────────────────────────────────

    /**
     * Converts a base key into a rider-scoped key by appending a normalised
     * version of the rider's email address.
     *
     * Example: "order_status" + rider@buyngo.com → "order_status_rider_buyngo_com"
     *
     * Falls back to the un-scoped base key when no rider session is active
     * (e.g. customer-side reads).
     */
    private static String getScopedKey(Context context, String baseKey) {
        String email = RiderSessionStore.getCurrentRiderEmail(context);
        if (email == null || email.trim().isEmpty()) {
            return baseKey;
        }
        String normalized = email.toLowerCase(Locale.US).replaceAll("[^a-z0-9]", "_");
        return baseKey + "_" + normalized;
    }

    /** Same scoping logic as {@link #getScopedKey} but for the history list. */
    private static String getScopedHistoryKey(Context context) {
        String email = RiderSessionStore.getCurrentRiderEmail(context);
        if (email == null || email.trim().isEmpty()) {
            return KEY_DELIVERY_HISTORY;
        }
        String normalized = email.toLowerCase(Locale.US).replaceAll("[^a-z0-9]", "_");
        return KEY_DELIVERY_HISTORY + "_" + normalized;
    }

    // ── Inner data classes ──────────────────────────────────────────────────

    /** Immutable snapshot of a current order (used by dashboard and status update). */
    static final class OrderInfo {
        final String orderId;
        final String customerName;
        final String customerAddress;
        final String status;

        OrderInfo(String orderId, String customerName, String customerAddress, String status) {
            this.orderId          = orderId;
            this.customerName     = customerName;
            this.customerAddress  = customerAddress;
            this.status           = status;
        }
    }

    /** Immutable record of a completed delivery (used by history screen and profile). */
    static final class DeliveryRecord {
        final String orderId;
        final String customerName;
        final String customerAddress;
        final long   deliveredAt;  // epoch millis

        DeliveryRecord(String orderId, String customerName,
                       String customerAddress, long deliveredAt) {
            this.orderId          = orderId;
            this.customerName     = customerName;
            this.customerAddress  = customerAddress;
            this.deliveredAt      = deliveredAt;
        }
    }
}
