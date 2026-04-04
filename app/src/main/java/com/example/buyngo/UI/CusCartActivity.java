package com.example.buyngo.UI;

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

        displayCartItems();

        checkoutButton.setOnClickListener(v -> {
            List<CartStore.CartItem> items = CartStore.getCartItems(this);
            if (items.isEmpty()) {
                Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            // Debug: show number of items
            Toast.makeText(this, "Checking out " + items.size() + " items", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, CusCheckoutActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        displayCartItems();
    }

    // Display all items in shopping cart
    private void displayCartItems() {
        cartItemsContainer.removeAllViews();

        List<CartStore.CartItem> items = CartStore.getCartItems(this);

        if (items.isEmpty()) {
            TextView emptyMsg = new TextView(this);
            emptyMsg.setText("Your cart is empty. Add items from the shop.");
            emptyMsg.setTextSize(14);
            emptyMsg.setPadding(16, 32, 16, 32);
            emptyMsg.setGravity(android.view.Gravity.CENTER);
            cartItemsContainer.addView(emptyMsg);
            totalPriceText.setText("$0.00");
            checkoutButton.setEnabled(false);
            checkoutButton.setAlpha(0.5f);
            return;
        }

        checkoutButton.setEnabled(true);
        checkoutButton.setAlpha(1f);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (CartStore.CartItem item : items) {
            android.view.View itemView = inflater.inflate(R.layout.item_cart, cartItemsContainer, false);

            TextView itemName = itemView.findViewById(R.id.cartItemName);
            TextView itemQty = itemView.findViewById(R.id.cartItemQty);
            TextView itemPrice = itemView.findViewById(R.id.cartItemPrice);
            Button removeBtn = itemView.findViewById(R.id.removeBtn);

            itemName.setText(item.name);
            itemQty.setText("Qty: " + item.quantity);
            itemPrice.setText(String.format("Rs. %.2f", item.getTotal()));

            removeBtn.setOnClickListener(v -> {
                CartStore.removeFromCart(this, item.productId);
                Toast.makeText(this, item.name + " removed from cart", Toast.LENGTH_SHORT).show();
                displayCartItems();
            });

            cartItemsContainer.addView(itemView);
        }

        double total = CartStore.getCartTotal(this);
        totalPriceText.setText(String.format("Rs. %.2f", total));
    }
}