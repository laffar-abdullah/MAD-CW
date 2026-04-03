package com.example.buyngo.Utils;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * One-time utility to remove old demo orders from Firebase.
 * Usage: Call cleanupOldOrders() once when needed.
 */
public class FirebaseOrderCleanup {
    private static final String TAG = "FirebaseOrderCleanup";
    private static final FirebaseDatabase db = FirebaseDatabase.getInstance("https://buyngo-5b43e-default-rtdb.firebaseio.com/");

    /**
     * Removes specific old order IDs from Firebase
     */
    public static void cleanupOldOrders() {
        String[] orderIdsToDelete = {
                "BNG-001",
                "ORD-1775149636946",
                "ORD-1775203846042",
                "ORD-1775210104399",
                "ORD-1775212292735",
                "ORD-1775214024285",
                "ORD-1775214047385",
                "ORD-1775215098069",
                "ORD-1775215301426",
                "ORD-1775215647551",
                "ORD-1775216036474",
                "UNKNOWN-ORDER"
        };

        for (String orderId : orderIdsToDelete) {
            db.getReference("orders").child(orderId).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Successfully deleted order: " + orderId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to delete order " + orderId + ": " + e.getMessage());
                    });
        }
    }

    /**
     * Removes all orders from Firebase (use with caution!)
     */
    public static void clearAllOrders() {
        db.getReference("orders").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                    orderSnapshot.getRef().removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Successfully deleted order: " + orderSnapshot.getKey());
                            });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to clear orders: " + error.getMessage());
            }
        });
    }

    /**
     * Migrates orders with "Confirmed" status but assigned to a rider
     * to "Awaiting Pickup" status so riders can see and update them
     */
    public static void migrateConfirmedOrdersToAwaitingPickup() {
        db.getReference("orders").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                    String status = orderSnapshot.child("status").getValue(String.class);
                    String assignedRiderEmail = orderSnapshot.child("assignedRiderEmail").getValue(String.class);
                    String orderId = orderSnapshot.getKey();

                    // If order is "Confirmed" AND has been assigned to a rider, migrate to "Awaiting Pickup"
                    if ("Confirmed".equals(status) && assignedRiderEmail != null && !assignedRiderEmail.isEmpty()) {
                        orderSnapshot.getRef().child("status").setValue("Awaiting Pickup")
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "✓ Migrated order " + orderId + " from 'Confirmed' to 'Awaiting Pickup'");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to migrate order " + orderId + ": " + e.getMessage());
                                });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to migrate orders: " + error.getMessage());
            }
        });
    }
}
