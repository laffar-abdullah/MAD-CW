ackage com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.buyngo.R;
import com.example.buyngo.Store.CartStore;

import java.util.List;


public class CusCartActivity extends AppCompatActivity {
    private LinearLayout cartItemsContainer;
    private TextView totalPriceText;
    private Button checkoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_cart);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        cartItemsContainer = findViewById(R.id.cartItemsContainer);
        totalPriceText = findViewById(R.id.totalPriceText);
        checkoutButton = findViewById(R.id.checkoutButton);

        // Display current cart items from local storage
        displayCartItems();

        // When "Checkout" tapped, validate cart and go to payment screen
        checkoutButton.setOnClickListener(v -> {
            // Get items from CartStore (local storage)
            List<CartStore.CartItem> items = CartStore.getCartItems(this);
            if (items.isEmpty()) {
                Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            // Go to checkout (here Order will be created and saved to Firebase)
            startActivity(new Intent(this, CusCheckoutActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh cart display when coming back to this screen
        displayCartItems();
    }

    /**
     * Read cart items from local storage and display on screen
     */
    private void displayCartItems() {
        // STEP 1: Clear screen of old cart items (in case items were removed)
        cartItemsContainer.removeAllViews();

        // STEP 2: Get all cart items from CartStore (local phone storage, NOT Firebase)
        List<CartStore.CartItem> items = CartStore.getCartItems(this);

        // STEP 3: Check if cart is empty
        if (items.isEmpty()) {
            // STEP 4: Create "empty cart" message
            TextView emptyMsg = new TextView(this);
            emptyMsg.setText("Your cart is empty. Add items from the shop.");
            emptyMsg.setTextSize(14);
            emptyMsg.setPadding(16, 32, 16, 32);
            emptyMsg.setGravity(android.view.Gravity.CENTER);
            // STEP 5: Add message to screen
            cartItemsContainer.addView(emptyMsg);
            // STEP 6: Set total price to $0
            totalPriceText.setText("$0.00");
            // STEP 7: Disable checkout button (can't checkout with empty cart)
            checkoutButton.setEnabled(false);
            // STEP 8: Make checkout button look disabled (50% transparent)
            checkoutButton.setAlpha(0.5f);
            return;
        }

        // STEP 9: If cart has items, enable checkout button
        checkoutButton.setEnabled(true);
        // STEP 10: Make checkout button look enabled (100% visible)
        checkoutButton.setAlpha(1f);

        // STEP 11: Create layout inflater to make cart item cards
        LayoutInflater inflater = LayoutInflater.from(this);
        
        // STEP 12: Loop through each item in cart
        for (CartStore.CartItem item : items) {
            // STEP 13: Create a card for this cart item from XML layout
            android.view.View itemView = inflater.inflate(R.layout.item_cart, cartItemsContainer, false);

            // STEP 14: Get UI elements from card (name, quantity, price, remove button)
            TextView itemName = itemView.findViewById(R.id.cartItemName);
            TextView itemQty = itemView.findViewById(R.id.cartItemQty);
            TextView itemPrice = itemView.findViewById(R.id.cartItemPrice);
            Button removeBtn = itemView.findViewById(R.id.removeBtn);

            // STEP 15: Set product name on card
            itemName.setText(item.name);
            // STEP 16: Set quantity on card (e.g., "Qty: 3")
            itemQty.setText("Qty: " + item.quantity);
            // STEP 17: Calculate total price for this item (price * quantity) and set on card
            itemPrice.setText(String.format("Rs. %.2f", item.getTotal()));

            // STEP 18: When customer clicks "Remove" button
            removeBtn.setOnClickListener(v -> {
                // STEP 19: Delete item from CartStore using product ID
                CartStore.removeFromCart(this, item.productId);
                // STEP 20: Show confirmation message to customer
                Toast.makeText(this, item.name + " removed from cart", Toast.LENGTH_SHORT).show();
                // STEP 21: Refresh cart display (item will be gone, total updated)
                displayCartItems();
            });

            // STEP 22: Add card to screen
            cartItemsContainer.addView(itemView);
        }

        // STEP 23: Calculate total price of all items in cart
        double total = CartStore.getCartTotal(this);
        // STEP 24: Set total price text on screen
        totalPriceText.setText(String.format("Rs. %.2f", total));
    }
}
