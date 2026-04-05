ackage com.example.buyngo.Model;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 *                              USER MODEL FILE
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * WHAT THIS FILE DOES:
 * This file defines the structure of customer profile data. Think of it as a
 * TEMPLATE that tells Firebase what fields should exist for each user.
 * 
 * HOW IT CONNECTS TO FIREBASE:
 * 1. When customer SIGNS UP → CusSignupActivity creates a User object
 * 2. Fills in name, email, phone, address from form
 * 3. Calls Firebase to save: database.getReference("users").child(userId).setValue(user)
 * 4. Firebase automatically converts this User object to JSON and stores it
 * 5. Result: User profile visible in Firebase Dashboard under /users/{userId}
 * 
 * HOW CUSTOMER MODULE USES IT:
 * - CusSignupActivity: Creates and saves new user during registration
 * - CusLoginActivity: Verifies email/password against Firebase Auth
 * - CusProfileActivity: Retrieves and displays user info on profile screen
 * - CusCheckoutActivity: Gets user's address to pre-fill delivery address
 * 
 * DATA FLOW:
 * Customer Signup Form → User Model → Firebase Database → /users/{userId}/
 *                                  ↓
 *                        Profile Screen Shows Data
 *                                  ↓
 *                        Checkout Uses Address
 * 
 * IMPORTANT FIELDS:
 * - userId: Unique identifier (matched with Firebase Auth UID)
 * - email: Used for login and order notifications
 * - fullName: Displayed on receipts and profiles
 * - phoneNumber: For rider to contact during delivery
 * - address: Pre-filled at checkout for delivery
 * - registrationDate: When customer joined (for admin reports)
 * ═══════════════════════════════════════════════════════════════════════════════
 */
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
