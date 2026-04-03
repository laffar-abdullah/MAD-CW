package com.example.buyngo.Store;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

// Manages shopping cart data using SharedPreferences and Gson for persistence
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
        List<CartItem> items = getCartItems(context);

        // Check if product already exists in cart
        boolean found = false;
        for (CartItem item : items) {
            if (item.productId.equals(productId)) {
                // If exists, increment quantity
                item.quantity += quantity;
                found = true;
                break;
            }
        }

        // If not found, add new item
        if (!found) {
            items.add(new CartItem(productId, name, category, price, quantity));
        }

        saveCartItems(context, items);
    }

    /**
     * Removes a product from the cart.
     * 
     * @param context Android context
     * @param productId Product ID to remove
     */
    public static void removeFromCart(Context context, String productId) {
        List<CartItem> items = getCartItems(context);
        items.removeIf(item -> item.productId.equals(productId));
        saveCartItems(context, items);
    }

    /**
     * Gets all items currently in the cart.
     * 
     * @param context Android context
     * @return List of CartItem objects
     */
    public static List<CartItem> getCartItems(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(CART_PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(CART_ITEMS_KEY, "[]");
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
        List<CartItem> items = getCartItems(context);
        double total = 0;
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
        saveCartItems(context, new ArrayList<>());
    }

    /**
     * Saves cart items to SharedPreferences using JSON serialization.
     * 
     * @param context Android context
     * @param items List of cart items to save
     */
    private static void saveCartItems(Context context, List<CartItem> items) {
        SharedPreferences prefs = context.getSharedPreferences(CART_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String json = gson.toJson(items);
        editor.putString(CART_ITEMS_KEY, json);
        editor.apply();
    }
}
