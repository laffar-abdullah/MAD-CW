package com.example.buyngo.UI;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


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
    static final String STATUS_DELIVERED   = "Delivered";

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

    
    private static String getScopedKey(Context context, String baseKey) {
        String email = RiderSessionStore.getCurrentRiderEmail(context);
        if (email == null || email.trim().isEmpty()) {
            return baseKey;
        }
        String normalized = email.toLowerCase(Locale.US).replaceAll("[^a-z0-9]", "_");
        return baseKey + "_" + normalized;
    }

    
    private static String getScopedHistoryKey(Context context) {
        String email = RiderSessionStore.getCurrentRiderEmail(context);
        if (email == null || email.trim().isEmpty()) {
            return KEY_DELIVERY_HISTORY;
        }
        String normalized = email.toLowerCase(Locale.US).replaceAll("[^a-z0-9]", "_");
        return KEY_DELIVERY_HISTORY + "_" + normalized;
    }

    // ── Inner data classes ──────────────────────────────────────────────────

    
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

