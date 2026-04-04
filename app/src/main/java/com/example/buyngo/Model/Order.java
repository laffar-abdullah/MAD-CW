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

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRiderId() { return riderId; }
    public void setRiderId(String riderId) { this.riderId = riderId; }

    public String getRiderName() { return riderName; }
    public void setRiderName(String riderName) { this.riderName = riderName; }

    public String getAssignedRiderEmail() { return assignedRiderEmail; }
    public void setAssignedRiderEmail(String assignedRiderEmail) { this.assignedRiderEmail = assignedRiderEmail; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getItemsAsString() {
        // First try to get items from itemsList (Firebase deserialized data)
        Map<String, Integer> itemsToUse = new HashMap<>();
        
        if (itemsList != null && !itemsList.isEmpty()) {
            for (OrderItem item : itemsList) {
                itemsToUse.put(item.name, item.quantity);
            }
        } else if (items != null && !items.isEmpty()) {
            itemsToUse = items;
        }
        
        if (itemsToUse.isEmpty()) {
            return "No items";
        }
        
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : itemsToUse.entrySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(entry.getValue()).append("x ").append(entry.getKey());
        }
        return sb.toString();
    }

    public String getTotalFormatted() {
        return String.format("$%.2f", totalAmount);
    }
}
