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
    private static final String PREFS_NAME = "buyngo_order_status";
    private static final String KEY_ORDER_STATUS = "order_status";
    private static final String KEY_ORDER_ID = "order_id";
    private static final String KEY_CUSTOMER_NAME = "customer_name";
    private static final String KEY_CUSTOMER_ADDRESS = "customer_address";
    private static final String KEY_DELIVERY_HISTORY = "delivery_history";
    static final String STATUS_PICKED_UP = "Picked Up";
    static final String STATUS_ON_THE_WAY = "On the Way";
    static final String STATUS_DELIVERED = "Delivered";
    private static final String DEFAULT_STATUS = "Awaiting Pickup";
    private static final String DEFAULT_ORDER_ID = "BNG-001";
    private static final String DEFAULT_CUSTOMER_NAME = "Alice Johnson";
    private static final String DEFAULT_CUSTOMER_ADDRESS = "12 Main Street, Springfield";

    private OrderStatusStore() {
        // Utility class
    }

    static void setStatus(Context context, String status) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Rider screens read the scoped value; customer tracking keeps using the global fallback.
        preferences.edit()
                .putString(KEY_ORDER_STATUS, status)
                .putString(getScopedKey(context, KEY_ORDER_STATUS), status)
                .apply();
    }

    static boolean updateStatus(Context context, String newStatus) {
        // Only allow forward progression through rider delivery lifecycle.
        String currentStatus = getStatus(context);
        if (!isValidTransition(currentStatus, newStatus)) {
            return false;
        }

        setStatus(context, newStatus);
        // Delivered orders are archived into delivery history once.
        if (STATUS_DELIVERED.equals(newStatus) && !STATUS_DELIVERED.equals(currentStatus)) {
            appendDeliveryHistory(context);
        }
        return true;
    }

    static String getStatus(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Rider screens see their own status copy; customer screens fall back to the shared one.
        String scopedStatus = preferences.getString(getScopedKey(context, KEY_ORDER_STATUS), null);
        if (scopedStatus != null) {
            return scopedStatus;
        }

        return preferences.getString(KEY_ORDER_STATUS, DEFAULT_STATUS);
    }

    static OrderInfo getCurrentOrder(Context context) {
        // Rider dashboard, status update, and profile all read this shared order snapshot.
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String orderId = preferences.getString(getScopedKey(context, KEY_ORDER_ID), preferences.getString(KEY_ORDER_ID, DEFAULT_ORDER_ID));
        String customerName = preferences.getString(getScopedKey(context, KEY_CUSTOMER_NAME), preferences.getString(KEY_CUSTOMER_NAME, DEFAULT_CUSTOMER_NAME));
        String customerAddress = preferences.getString(getScopedKey(context, KEY_CUSTOMER_ADDRESS), preferences.getString(KEY_CUSTOMER_ADDRESS, DEFAULT_CUSTOMER_ADDRESS));
        String status = getStatus(context);
        return new OrderInfo(orderId, customerName, customerAddress, status);
    }

    static List<DeliveryRecord> getDeliveryHistory(Context context) {
        // Delivery history is stored as JSON so it can be rendered as cards later.
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String rawHistory = preferences.getString(getScopedHistoryKey(context), "[]");
        List<DeliveryRecord> records = new ArrayList<>();

        try {
            JSONArray history = new JSONArray(rawHistory);
            for (int i = 0; i < history.length(); i++) {
                JSONObject item = history.getJSONObject(i);
                records.add(new DeliveryRecord(
                        item.optString("orderId", DEFAULT_ORDER_ID),
                        item.optString("customerName", DEFAULT_CUSTOMER_NAME),
                        item.optString("customerAddress", DEFAULT_CUSTOMER_ADDRESS),
                        item.optLong("deliveredAt", System.currentTimeMillis())
                ));
            }
        } catch (JSONException ignored) {
            // Keep app usable even if old/corrupted history exists.
        }

        return records;
    }

    static void initializeDefaultsIfMissing(Context context) {
        // Seed the order snapshot used by dashboard and status update screens.
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String orderIdKey = getScopedKey(context, KEY_ORDER_ID);
        String customerNameKey = getScopedKey(context, KEY_CUSTOMER_NAME);
        String customerAddressKey = getScopedKey(context, KEY_CUSTOMER_ADDRESS);
        String statusKey = getScopedKey(context, KEY_ORDER_STATUS);

        SharedPreferences.Editor editor = preferences.edit();
        if (!preferences.contains(orderIdKey)) {
            editor.putString(orderIdKey, DEFAULT_ORDER_ID);
        }
        if (!preferences.contains(customerNameKey)) {
            editor.putString(customerNameKey, DEFAULT_CUSTOMER_NAME);
        }
        if (!preferences.contains(customerAddressKey)) {
            editor.putString(customerAddressKey, DEFAULT_CUSTOMER_ADDRESS);
        }
        if (!preferences.contains(statusKey)) {
            editor.putString(statusKey, DEFAULT_STATUS);
        }

        // Keep the original shared keys as a fallback for customer-side tracking.
        if (!preferences.contains(KEY_ORDER_ID)) {
            editor.putString(KEY_ORDER_ID, DEFAULT_ORDER_ID);
        }
        if (!preferences.contains(KEY_CUSTOMER_NAME)) {
            editor.putString(KEY_CUSTOMER_NAME, DEFAULT_CUSTOMER_NAME);
        }
        if (!preferences.contains(KEY_CUSTOMER_ADDRESS)) {
            editor.putString(KEY_CUSTOMER_ADDRESS, DEFAULT_CUSTOMER_ADDRESS);
        }
        if (!preferences.contains(KEY_ORDER_STATUS)) {
            editor.putString(KEY_ORDER_STATUS, DEFAULT_STATUS);
        }

        editor.apply();
    }

    private static boolean isValidTransition(String currentStatus, String newStatus) {
        // Allow idempotent writes when user taps the current status again.
        if (currentStatus.equals(newStatus)) {
            return true;
        }

        if (DEFAULT_STATUS.equals(currentStatus)) {
            return STATUS_PICKED_UP.equals(newStatus);
        }
        if (STATUS_PICKED_UP.equals(currentStatus)) {
            return STATUS_ON_THE_WAY.equals(newStatus);
        }
        if (STATUS_ON_THE_WAY.equals(currentStatus)) {
            return STATUS_DELIVERED.equals(newStatus);
        }

        return false;
    }

    private static void appendDeliveryHistory(Context context) {
        // Snapshot delivered order details for history screen rendering.
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String orderId = preferences.getString(KEY_ORDER_ID, DEFAULT_ORDER_ID);
        String customerName = preferences.getString(KEY_CUSTOMER_NAME, DEFAULT_CUSTOMER_NAME);
        String customerAddress = preferences.getString(KEY_CUSTOMER_ADDRESS, DEFAULT_CUSTOMER_ADDRESS);
        String scopedKey = getScopedHistoryKey(context);
        String rawHistory = preferences.getString(scopedKey, "[]");

        try {
            JSONArray history = new JSONArray(rawHistory);
            JSONObject item = new JSONObject();
            item.put("orderId", orderId);
            item.put("customerName", customerName);
            item.put("customerAddress", customerAddress);
            item.put("deliveredAt", System.currentTimeMillis());
            history.put(item);
            preferences.edit().putString(scopedKey, history.toString()).apply();
        } catch (JSONException ignored) {
            // Ignore and avoid crashing if storage contains malformed JSON.
        }
    }

    private static String getScopedHistoryKey(Context context) {
        String riderEmail = RiderSessionStore.getCurrentRiderEmail(context);
        if (riderEmail == null || riderEmail.trim().isEmpty()) {
            return KEY_DELIVERY_HISTORY;
        }

        String normalized = riderEmail
                .toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]", "_");
        return KEY_DELIVERY_HISTORY + "_" + normalized;
    }

    private static String getScopedKey(Context context, String baseKey) {
        String riderEmail = RiderSessionStore.getCurrentRiderEmail(context);
        if (riderEmail == null || riderEmail.trim().isEmpty()) {
            return baseKey;
        }

        String normalized = riderEmail
                .toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]", "_");
        return baseKey + "_" + normalized;
    }

    static final class OrderInfo {
        final String orderId;
        final String customerName;
        final String customerAddress;
        final String status;

        OrderInfo(String orderId, String customerName, String customerAddress, String status) {
            this.orderId = orderId;
            this.customerName = customerName;
            this.customerAddress = customerAddress;
            this.status = status;
        }
    }

    static final class DeliveryRecord {
        final String orderId;
        final String customerName;
        final String customerAddress;
        final long deliveredAt;

        DeliveryRecord(String orderId, String customerName, String customerAddress, long deliveredAt) {
            this.orderId = orderId;
            this.customerName = customerName;
            this.customerAddress = customerAddress;
            this.deliveredAt = deliveredAt;
        }
    }
}