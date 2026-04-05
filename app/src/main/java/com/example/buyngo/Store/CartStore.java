package com.example.buyngo.Store;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 *                              CART STORE FILE
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * WHAT THIS FILE DOES:
 * This file manages the shopping cart. It stores what items the customer has
 * added to cart LOCALLY on their phone (not in Firebase). Think of it as a
 * temporary holding area until customer clicks "Checkout".
 * 
 * HOW IT CONNECTS TO FIREBASE:
 * CartStore is SEPARATE from Firebase. It stores data locally using SharedPreferences.
 * 
 * FLOW:
 * 1. Customer on CusHomeActivity taps "Add to Cart"
 * 2. Product data (name, price, quantity) saved to CartStore locally
 * 3. CartStore stores as JSON in phone memory using SharedPreferences
 * 4. CusCartActivity reads from CartStore and displays items
 * 5. Customer clicks "Checkout"
 * 6. CusCheckoutActivity reads cart from CartStore
 * 7. Creates Order object with cart items
 * 8. Saves Order to Firebase (NOW it goes to cloud)
 * 9. Cart cleared from phone storage
 * 
 * WHY NOT USE FIREBASE FOR CART?
 * - Faster: Local storage is quicker than cloud
 * - Cheaper: Saves Firebase database bandwidth
 * - Better UX: Customer can browse products without internet
 * - Private: Cart is only this customer's, not synced to other devices */
public class CartStore {
    private static final String CART_PREFS = "cart_preferences";
    private static final String CART_ITEMS_KEY = "cart_items";
    private static final Gson gson = new Gson();

    // Represents a single item in the shopping cart
    public static class CartItem {
        public String productId;
        public String name;
        public String category;
        public double price;
        public int quantity;

        public CartItem(String productId, String name, String category, double price, int quantity) {
            this.productId = productId;
            this.name = name;
            this.category = category;
            this.price = price;
            this.quantity = quantity;
        }

        // Calculate total for this item (price * quantity)
        public double getTotal() {
            return price * quantity;
        }
    }

    /**
     * Adds a product to the cart or increments its quantity if already exists.
     * 
     * @param context Android context
     * @param productId Unique product identifier
     * @param name Product name
     * @param category Product category
     * @param price Product price
     * @param quantity Quantity to add
     */
    public static void addToCart(Context context, String productId, String name, String category, double price, int quantity) {
        // Get existing cart items from phone storage
        List<CartItem> items = getCartItems(context);

        // Check if product already exists in cart
        // If yes, we just increase its quantity instead of adding duplicate
        boolean found = false;
        for (CartItem item : items) {
            if (item.productId.equals(productId)) {
                // Already in cart - just add more quantity
                item.quantity += quantity;
                found = true;
                break;
            }
        }

        // If product not found, add it as new item
        if (!found) {
            items.add(new CartItem(productId, name, category, price, quantity));
        }

        // Save updated cart back to phone storage
        saveCartItems(context, items);
    }

    /**
     * Removes a product from the cart.
     * 
     * @param context Android context
     * @param productId Product ID to remove
     */
    public static void removeFromCart(Context context, String productId) {
        // Get all cart items
        List<CartItem> items = getCartItems(context);
        // Remove the item with matching product ID
        items.removeIf(item -> item.productId.equals(productId));
        // Save updated cart
        saveCartItems(context, items);
    }

    /**
     * Gets all items currently in the cart.
     * 
     * @param context Android context
     * @return List of CartItem objects
     */
    public static List<CartItem> getCartItems(Context context) {
        // Read from SharedPreferences (phone local storage)
        SharedPreferences prefs = context.getSharedPreferences(CART_PREFS, Context.MODE_PRIVATE);
        // Get JSON string (or empty array if nothing exists)
        String json = prefs.getString(CART_ITEMS_KEY, "[]");
        // Convert JSON back to List of CartItems
        Type type = new TypeToken<List<CartItem>>() {}.getType();
        return gson.fromJson(json, type);
    }

    /**
     * Calculates the total price of all items in cart.
     * 
     * @param context Android context
     * @return Total cart price
     */
    public static double getCartTotal(Context context) {
        // Get all items in cart
        List<CartItem> items = getCartItems(context);
        double total = 0;
        // Sum up: (price * quantity) for each item
        for (CartItem item : items) {
            total += item.getTotal();
        }
        return total;
    }

    /**
     * Gets the number of items in cart.
     * 
     * @param context Android context
     * @return Number of unique items
     */
    public static int getCartItemCount(Context context) {
        return getCartItems(context).size();
    }

    /**
     * Clears the entire cart.
     * 
     * @param context Android context
     */
    public static void clearCart(Context context) {
        // Reset cart to empty list
        saveCartItems(context, new ArrayList<>());
    }

    /**
     * Saves cart items to SharedPreferences using JSON serialization.
     * This stores cart in phone's local storage so it persists even if app closes.
     * 
     * @param context Android context
     * @param items List of cart items to save
     */
    private static void saveCartItems(Context context, List<CartItem> items) {
        // Get access to phone's local storage
        SharedPreferences prefs = context.getSharedPreferences(CART_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        // Convert list to JSON string
        String json = gson.toJson(items);
        // Save JSON to local storage
        editor.putString(CART_ITEMS_KEY, json);
        // Commit the save operation
        editor.apply();
    }
}
