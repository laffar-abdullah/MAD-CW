package com.example.buyngo.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Represents a customer order with items and delivery status
public class Order {
    private String orderId;
    private String customerId;
    private String customerName;
    private String customerAddress;  // NEW: Customer's delivery address
    private String customerPhone;     // NEW: Customer's contact number
    private Map<String, Integer> items;
    private List<OrderItem> itemsList;  // New field for Firebase serialization
    private double totalAmount;
    private String status;
    private String riderId;
    private String riderName;
    private String assignedRiderEmail;
    private String paymentMethod;  // NEW: Payment method (e.g., "Card", "Cash")
    private long createdAt;
    private long updatedAt;

    // Nested class for Firebase serialization
    public static class OrderItem {
        public String name;
        public int quantity;

        public OrderItem() {}

        public OrderItem(String name, int quantity) {
            this.name = name;
            this.quantity = quantity;
        }
    }

    public Order() {
        this.items = new HashMap<>();
        this.itemsList = new ArrayList<>();
        this.status = "Pending";
        this.riderId = "";
        this.riderName = "";
        this.assignedRiderEmail = "";
        this.paymentMethod = "";
    }

    public Order(String orderId, String customerId, String customerName,
                 Map<String, Integer> items, double totalAmount) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.items = items != null ? items : new HashMap<>();
        
        // Convert map to list for Firebase serialization
        this.itemsList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : this.items.entrySet()) {
            this.itemsList.add(new OrderItem(entry.getKey(), entry.getValue()));
        }
        
        this.totalAmount = totalAmount;
        this.status = "Pending";
        this.riderId = "";
        this.riderName = "";
        this.assignedRiderEmail = "";
        this.paymentMethod = "";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public Map<String, Integer> getItems() { 
        // Convert itemsList to items map when retrieving
        if (items == null || items.isEmpty()) {
            items = new HashMap<>();
            if (itemsList != null) {
                for (OrderItem item : itemsList) {
                    items.put(item.name, item.quantity);
                }
            }
        }
        return items; 
    }
    
    public void setItems(Map<String, Integer> items) { 
        this.items = items;
        // Also update itemsList
        this.itemsList = new ArrayList<>();
        if (items != null) {
            for (Map.Entry<String, Integer> entry : items.entrySet()) {
                this.itemsList.add(new OrderItem(entry.getKey(), entry.getValue()));
            }
        }
    }

    public List<OrderItem> getItemsList() { return itemsList; }
    public void setItemsList(List<OrderItem> itemsList) { 
        this.itemsList = itemsList;
        // Also update items map
        this.items = new HashMap<>();
        if (itemsList != null) {
            for (OrderItem item : itemsList) {
                this.items.put(item.name, item.quantity);
            }
        }
    }

    public double getTotalAmount() { 
        android.util.Log.d("Order", "getTotalAmount() for " + orderId + " = " + totalAmount);
        return totalAmount; 
    }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRiderId() { return riderId; }
    public void setRiderId(String riderId) { this.riderId = riderId; }

    public String getRiderName() { return riderName; }
    public void setRiderName(String riderName) { this.riderName = riderName; }

    public String getAssignedRiderEmail() { return assignedRiderEmail; }
    public void setAssignedRiderEmail(String assignedRiderEmail) { this.assignedRiderEmail = assignedRiderEmail; }

    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getItemsAsString() {
        // First try to get items from itemsList (Firebase deserialized data)
        Map<String, Integer> itemsToUse = new HashMap<>();
        
        android.util.Log.d("Order", "getItemsAsString() called for order " + orderId);
        android.util.Log.d("Order", "  itemsList: " + (itemsList != null ? itemsList.size() + " items" : "null"));
        android.util.Log.d("Order", "  items map: " + (items != null ? items.size() + " items" : "null"));
        
        if (itemsList != null && !itemsList.isEmpty()) {
            android.util.Log.d("Order", "  Using itemsList");
            for (OrderItem item : itemsList) {
                itemsToUse.put(item.name, item.quantity);
                android.util.Log.d("Order", "    - " + item.quantity + "x " + item.name);
            }
        } else if (items != null && !items.isEmpty()) {
            android.util.Log.d("Order", "  Using items map");
            itemsToUse = items;
            for (Map.Entry<String, Integer> e : items.entrySet()) {
                android.util.Log.d("Order", "    - " + e.getValue() + "x " + e.getKey());
            }
        }
        
        if (itemsToUse.isEmpty()) {
            android.util.Log.d("Order", "  Result: No items");
            return "No items";
        }
        
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : itemsToUse.entrySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(entry.getValue()).append("x ").append(entry.getKey());
        }
        android.util.Log.d("Order", "  Result: " + sb.toString());
        return sb.toString();
    }

    public String getTotalFormatted() {
        String formatted = String.format("Rs. %.2f", totalAmount);
        android.util.Log.d("Order", "getTotalFormatted() for " + orderId + " = " + formatted + " (totalAmount: " + totalAmount + ")");
        return formatted;
    }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
