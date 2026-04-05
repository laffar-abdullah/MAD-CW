ackage com.example.buyngo.Store;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

// Manages shopping cart stored locally on phone using SharedPreferences
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

    
    public static void addToCart(Context context, String productId, String name, String category, double price, int quantity) {
        // STEP 1: Read existing cart items from phone local storage
        List<CartItem> items = getCartItems(context);

        // STEP 2: Check if product already exists in cart
        // STEP 3: If yes, we just increase its quantity instead of adding duplicate
        boolean found = false;
        for (CartItem item : items) {
            // STEP 4: Compare product ID to see if product already in cart
            if (item.productId.equals(productId)) {
                // STEP 5: Product already in cart - increase its quantity
                item.quantity += quantity;
                // STEP 6: Mark as found so we don't add duplicate
                found = true;
                break;
            }
        }

        // STEP 7: If product not found, add it as new item
        if (!found) {
            // STEP 8: Create new CartItem with product details
            items.add(new CartItem(productId, name, category, price, quantity));
        }

        // STEP 9: Save updated cart back to phone storage
        saveCartItems(context, items);
    }

    
    public static void removeFromCart(Context context, String productId) {
        // STEP 1: Get all cart items from phone storage
        List<CartItem> items = getCartItems(context);
        // STEP 2: Remove the item with matching product ID using removeIf()
        items.removeIf(item -> item.productId.equals(productId));
        // STEP 3: Save updated cart (without the removed item) back to phone storage
        saveCartItems(context, items);
    }

    
    public static List<CartItem> getCartItems(Context context) {
        // STEP 1: Access phone's local storage using SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(CART_PREFS, Context.MODE_PRIVATE);
        // STEP 2: Get JSON string from storage (default to "[]" if nothing stored)
        String json = prefs.getString(CART_ITEMS_KEY, "[]");
        // STEP 3: Define what type to convert JSON into (List of CartItems)
        Type type = new TypeToken<List<CartItem>>() {}.getType();
        // STEP 4: Convert JSON string back to List of CartItem objects
        return gson.fromJson(json, type);
    }

    
    public static double getCartTotal(Context context) {
        // STEP 1: Get all items in cart from phone storage
        List<CartItem> items = getCartItems(context);
        // STEP 2: Start total at 0
        double total = 0;
        // STEP 3: Loop through each item in cart
        for (CartItem item : items) {
            // STEP 4: Add item's total (price * quantity) to running total
            total += item.getTotal();
        }
        // STEP 5: Return final total
        return total;
    }

    
    public static int getCartItemCount(Context context) {
        // STEP 1: Get all items from cart
        // STEP 2: Return count of how many unique items are in cart
        return getCartItems(context).size();
    }

    
    public static void clearCart(Context context) {
        // STEP 1: Save empty list to phone storage (this deletes all cart items)
        saveCartItems(context, new ArrayList<>());
    }

    
    private static void saveCartItems(Context context, List<CartItem> items) {
        // STEP 1: Get access to phone's local storage (SharedPreferences)
        SharedPreferences prefs = context.getSharedPreferences(CART_PREFS, Context.MODE_PRIVATE);
        // STEP 2: Create editor to modify storage
        SharedPreferences.Editor editor = prefs.edit();
        // STEP 3: Convert list of CartItems to JSON string
        String json = gson.toJson(items);
        // STEP 4: Store JSON string in local storage with key "cart_items"
        editor.putString(CART_ITEMS_KEY, json);
        // STEP 5: Commit/save the changes to phone storage
        editor.apply();
    }
}

