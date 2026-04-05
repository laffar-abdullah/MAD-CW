package com.example.buyngo.Model;

import java.io.Serializable;

// Represents an item in the shopping cart
public class CartItem implements Serializable {
    private String productId;
    private String productName;
    private double productPrice;
    private int quantity;
    private String category;

    public CartItem() {
    }

    public CartItem(String productId, String productName, double productPrice, int quantity, String category) {
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.quantity = quantity;
        this.category = category;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public double getProductPrice() { return productPrice; }
    public void setProductPrice(double productPrice) { this.productPrice = productPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getTotalPrice() {
        return productPrice * quantity;
    }
}
