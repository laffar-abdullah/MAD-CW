package com.example.buyngo.Model;
public class User {
    private String userId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String city;
    private long registrationDate;

    public User() {
    }

    public User(String userId, String email, String fullName, String phoneNumber, 
                String address, String city, long registrationDate) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.city = city;
        this.registrationDate = registrationDate;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public long getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(long registrationDate) { this.registrationDate = registrationDate; }
}
