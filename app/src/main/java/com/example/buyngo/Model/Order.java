package com.example.buyngo.Model;

import java.util.HashMap;
import java.util.Map;

// Represents a customer order with items and delivery status
public class Order {
    private String orderId;
    private String customerId;
    private String customerName;
    private Map<String, Integer> items;
    private double totalAmount;
    private String status;
    private String riderId;
    private String riderName;
    private String assignedRiderEmail;
    private long createdAt;
    private long updatedAt;

    public Order() {
        this.items = new HashMap<>();
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

    public Map<String, Integer> getItems() { return items; }
    public void setItems(Map<String, Integer> items) { this.items = items; }

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
        if (items == null || items.isEmpty()) {
            return "No items";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
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
