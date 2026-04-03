import com.google.firebase.database.FirebaseDatabase;

/**
 * Utility script to clear all orders from Firebase Realtime Database
 * Run this to delete all test/old orders
 * 
 * Usage: 
 * 1. Make sure Firebase SDK is in classpath
 * 2. Initialize Firebase
 * 3. Run this script
 */
public class ClearAllOrders {
    public static void main(String[] args) {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance(
                "https://buyngo-5b43e-default-rtdb.firebaseio.com/");
        
        firebaseDatabase.getReference("orders").removeValue()
                .addOnSuccessListener(aVoid -> {
                    System.out.println("✓ All orders cleared successfully!");
                })
                .addOnFailureListener(e -> {
                    System.out.println("✗ Failed to clear orders: " + e.getMessage());
                });
    }
}
