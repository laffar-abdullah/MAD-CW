package com.example.buyngo.Model;

public class Rider {
    private String riderId;
    private String riderName;
    private String phoneNumber;
    private String email;
    private String status; // Available, Busy, Offline
    private long registrationDate;

    // Default constructor required for Firebase
    public Rider() {
    }

    public Rider(String riderId, String riderName, String phoneNumber, String email, String status) {
        this.riderId = riderId;
        this.riderName = riderName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.status = status;
        this.registrationDate = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getRiderId() {
        return riderId;
    }

    public void setRiderId(String riderId) {
        this.riderId = riderId;
    }

    public String getRiderName() {
        return riderName;
    }

    public void setRiderName(String riderName) {
        this.riderName = riderName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(long registrationDate) {
        this.registrationDate = registrationDate;
    }
}
