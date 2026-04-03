package com.example.buyngo.UI;

import androidx.annotation.NonNull;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class FirebaseRiderRepository {

    private static final String TAG = "FIREBASE_RIDER_REPO";
    private static final String DATABASE_URL = "https://buyngo-5b43e-default-rtdb.firebaseio.com/";

    interface ResultCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    interface VoidCallback {
        void onSuccess();
        void onError(String message);
    }

    static final class RiderAccount {
        public String riderId;
        public String name;
        public String phone;
        public String email;
        public String password;
        public String vehicle;
        public String vehicleNumber;
        public String profileImageUrl;
        public long createdAt;

        public RiderAccount() {
            // Required by Firebase.
        }
    }

    static final class RiderOrder {
        public String orderId;
        public String customerName;
        public String customerAddress;
        public String status;
        public String assignedRiderEmail;
        public long updatedAt;
        public long deliveredAt;

        public RiderOrder() {
            // Required by Firebase.
        }
    }

    static final class RiderReview {
        public String reviewId;
        public String orderId;
        public String riderEmail;
        public String customerName;
        public int rating;
        public String comment;
        public long createdAt;

        public RiderReview() {
            // Required by Firebase.
        }
    }

    private static final String NODE_RIDERS = "riders";
    private static final String NODE_ORDERS = "orders";
    private static final String NODE_REVIEWS = "reviews";

    private FirebaseRiderRepository() {
    }

    private static DatabaseReference db() {
        return FirebaseDatabase.getInstance(DATABASE_URL).getReference();
    }

    private static String riderIdFromEmail(String email) {
        return email.toLowerCase(Locale.US).replaceAll("[^a-z0-9]", "_");
    }

    static void registerRider(
            String name,
            String phone,
            String vehicleType,
            String vehicleNumber,
            String email,
            String password,
            ResultCallback<RiderAccount> callback) {

        String riderId = riderIdFromEmail(email);
        DatabaseReference riderRef = db().child(NODE_RIDERS).child(riderId);
        Log.d(TAG, "registerRider() start path=" + NODE_RIDERS + "/" + riderId);
        riderRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Log.w(TAG, "registerRider() duplicate email. path=" + NODE_RIDERS + "/" + riderId);
                        callback.onError("A rider with this email already exists");
                        return;
                    }

                    RiderAccount account = new RiderAccount();
                    account.riderId = riderId;
                    account.name = name;
                    account.phone = phone;
                    account.email = email;
                    account.password = password;
                    account.vehicle = vehicleType;
                    account.vehicleNumber = vehicleNumber;
                    account.createdAt = System.currentTimeMillis();

                    riderRef.setValue(account)
                            .addOnSuccessListener(unused -> {
                                Log.i(TAG, "registerRider() write success path=" + NODE_RIDERS + "/" + riderId);
                                callback.onSuccess(account);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "registerRider() write failed", e);
                                callback.onError(
                                        e.getMessage() == null ? "Failed to register rider" : e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "registerRider() read/check failed", e);
                    callback.onError(
                            e.getMessage() == null ? "Failed to check rider" : e.getMessage());
                });
    }

    static void authenticateRider(String email, String password, ResultCallback<RiderAccount> callback) {
        String riderId = riderIdFromEmail(email);
        db().child(NODE_RIDERS).child(riderId).get()
                .addOnSuccessListener(snapshot -> {
                    RiderAccount account = snapshot.getValue(RiderAccount.class);
                    if (account == null) {
                        callback.onError("No rider found with this email");
                        return;
                    }
                    if (account.password == null || !account.password.equals(password)) {
                        callback.onError("Invalid rider credentials");
                        return;
                    }
                    callback.onSuccess(account);
                })
                .addOnFailureListener(e -> callback.onError(
                        e.getMessage() == null ? "Login failed" : e.getMessage()));
    }

    static void updateRiderProfileImage(String email, Uri imageUri, ResultCallback<RiderAccount> callback) {
        if (imageUri == null) {
            callback.onError("Please choose a profile picture");
            return;
        }

        String riderId = riderIdFromEmail(email);
        DatabaseReference riderRef = db().child(NODE_RIDERS).child(riderId);
        StorageReference imageRef = FirebaseStorage.getInstance()
                .getReference()
                .child("rider_profiles")
                .child(riderId)
                .child("profile.jpg");

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> riderRef.get()
                                .addOnSuccessListener(snapshot -> {
                                    RiderAccount account = snapshot.getValue(RiderAccount.class);
                                    if (account == null) {
                                        callback.onError("Rider account not found");
                                        return;
                                    }

                                    account.profileImageUrl = downloadUri.toString();
                                    riderRef.setValue(account)
                                            .addOnSuccessListener(unused -> callback.onSuccess(account))
                                            .addOnFailureListener(e -> callback.onError(
                                                    e.getMessage() == null ? "Failed to save profile picture" : e.getMessage()));
                                })
                                .addOnFailureListener(e -> callback.onError(
                                        e.getMessage() == null ? "Failed to load rider account" : e.getMessage())))
                        .addOnFailureListener(e -> callback.onError(
                                e.getMessage() == null ? "Failed to resolve profile picture URL" : e.getMessage())))
                .addOnFailureListener(e -> callback.onError(
                        e.getMessage() == null ? "Failed to upload profile picture" : e.getMessage()));
    }

    static void changeRiderPassword(
            String email,
            String currentPassword,
            String newPassword,
            ResultCallback<RiderAccount> callback) {

        String riderId = riderIdFromEmail(email);
        DatabaseReference riderRef = db().child(NODE_RIDERS).child(riderId);

        riderRef.get()
                .addOnSuccessListener(snapshot -> {
                    RiderAccount account = snapshot.getValue(RiderAccount.class);
                    if (account == null) {
                        callback.onError("Rider account not found");
                        return;
                    }
                    if (account.password == null || !account.password.equals(currentPassword)) {
                        callback.onError("Current password is incorrect");
                        return;
                    }

                    account.password = newPassword;
                    riderRef.setValue(account)
                            .addOnSuccessListener(unused -> callback.onSuccess(account))
                            .addOnFailureListener(e -> callback.onError(
                                    e.getMessage() == null ? "Failed to update password" : e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(
                        e.getMessage() == null ? "Failed to load rider account" : e.getMessage()));
    }

    static void getAllRiders(ResultCallback<List<RiderAccount>> callback) {
        db().child(NODE_RIDERS).get()
                .addOnSuccessListener(snapshot -> {
                    List<RiderAccount> riders = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        RiderAccount rider = child.getValue(RiderAccount.class);
                        if (rider != null) {
                            riders.add(rider);
                        }
                    }
                    riders.sort(Comparator.comparing(a -> a.name == null ? "" : a.name));
                    callback.onSuccess(riders);
                })
                .addOnFailureListener(e -> callback.onError(
                        e.getMessage() == null ? "Failed to load riders" : e.getMessage()));
    }

    static void assignOrderToRider(
            String orderId,
            String customerName,
            String customerAddress,
            String riderEmail,
            VoidCallback callback) {

        Log.d(TAG, "assignOrderToRider called - orderId: " + orderId + " | riderEmail: " + riderEmail);
        
        // Get all riders and find the one with matching email
        db().child(NODE_RIDERS).get().addOnSuccessListener(snapshot -> {
            String riderName = "Assigned Rider";
            for (DataSnapshot child : snapshot.getChildren()) {
                RiderAccount rider = child.getValue(RiderAccount.class);
                if (rider != null && rider.email != null && rider.email.equals(riderEmail)) {
                    riderName = rider.name != null ? rider.name : "Assigned Rider";
                    Log.d(TAG, "Found rider with name: " + riderName);
                    break;
                }
            }

            // Now update only the rider fields, preserving order data
            DatabaseReference orderRef = db().child(NODE_ORDERS).child(orderId);
            final String finalRiderName = riderName;
            
            Log.d(TAG, "Saving assignedRiderEmail: " + riderEmail + " for order: " + orderId);
            // Update rider-related fields AND set initial status to "Awaiting Pickup" for rider workflow
            orderRef.child("assignedRiderEmail").setValue(riderEmail);
            orderRef.child("riderName").setValue(finalRiderName);
            orderRef.child("status").setValue("Awaiting Pickup")  // Set rider's initial status
                    .addOnSuccessListener(unused -> {
                        Log.d(TAG, "✓ Order assigned successfully to rider: " + riderEmail + " with status: Awaiting Pickup");
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to assign order: " + e.getMessage());
                        callback.onError(
                            e.getMessage() == null ? "Failed to assign order" : e.getMessage());
                    });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to find rider: " + e.getMessage());
            callback.onError(
                e.getMessage() == null ? "Failed to find rider" : e.getMessage());
        });
    }

    static void getAssignedOrdersForRider(String riderEmail, ResultCallback<List<RiderOrder>> callback) {
        // Fetch all orders and filter by assignedRiderEmail on client-side to avoid index requirements
        Log.d(TAG, "getAssignedOrdersForRider called for email: " + riderEmail);
        db().child(NODE_ORDERS).get()
                .addOnSuccessListener(snapshot -> {
                    Log.d(TAG, "Fetched " + snapshot.getChildrenCount() + " total orders from Firebase");
                    List<RiderOrder> orders = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        RiderOrder order = child.getValue(RiderOrder.class);
                        if (order != null) {
                            Log.d(TAG, "Order: " + child.getKey() + " | assignedRiderEmail: " + order.assignedRiderEmail + " | status: " + order.status);
                            if (riderEmail.equals(order.assignedRiderEmail)) {
                                Log.d(TAG, "✓ MATCH FOUND for rider: " + riderEmail);
                                orders.add(order);
                            }
                        }
                    }
                    Log.d(TAG, "Found " + orders.size() + " orders for rider: " + riderEmail);
                    orders.sort((a, b) -> Long.compare(b.updatedAt, a.updatedAt));
                    callback.onSuccess(orders);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching orders: " + e.getMessage());
                    callback.onError(
                        e.getMessage() == null ? "Failed to load assigned orders" : e.getMessage());
                });
    }

    static void getLatestActiveOrderForRider(String riderEmail, ResultCallback<RiderOrder> callback) {
        getAssignedOrdersForRider(riderEmail, new ResultCallback<List<RiderOrder>>() {
            @Override
            public void onSuccess(List<RiderOrder> orders) {
                for (RiderOrder order : orders) {
                    if (!OrderStatusStore.STATUS_DELIVERED.equals(order.status)) {
                        callback.onSuccess(order);
                        return;
                    }
                }
                callback.onSuccess(null);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    static void updateOrderStatus(String orderId, String newStatus, VoidCallback callback) {
        DatabaseReference orderRef = db().child(NODE_ORDERS).child(orderId);
        orderRef.get()
                .addOnSuccessListener(snapshot -> {
                    RiderOrder order = snapshot.getValue(RiderOrder.class);
                    if (order == null) {
                        callback.onError("Order not found");
                        return;
                    }

                    if (!isValidTransition(order.status, newStatus)) {
                        callback.onError("Invalid status transition");
                        return;
                    }

                    order.status = newStatus;
                    order.updatedAt = System.currentTimeMillis();
                    if (OrderStatusStore.STATUS_DELIVERED.equals(newStatus)) {
                        order.deliveredAt = System.currentTimeMillis();
                    }

                    orderRef.setValue(order)
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError(
                                    e.getMessage() == null ? "Failed to update status" : e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(
                        e.getMessage() == null ? "Failed to read order" : e.getMessage()));
    }

    static void getDeliveredOrdersForRider(String riderEmail, ResultCallback<List<RiderOrder>> callback) {
        getAssignedOrdersForRider(riderEmail, new ResultCallback<List<RiderOrder>>() {
            @Override
            public void onSuccess(List<RiderOrder> orders) {
                List<RiderOrder> delivered = new ArrayList<>();
                for (RiderOrder order : orders) {
                    if (OrderStatusStore.STATUS_DELIVERED.equals(order.status)) {
                        delivered.add(order);
                    }
                }
                delivered.sort((a, b) -> Long.compare(b.deliveredAt, a.deliveredAt));
                callback.onSuccess(delivered);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    static void addReview(
            String orderId,
            String riderEmail,
            String customerName,
            int rating,
            String comment,
            VoidCallback callback) {

        DatabaseReference reviewsRef = db().child(NODE_REVIEWS);
        String reviewId = reviewsRef.push().getKey();
        if (reviewId == null) {
            callback.onError("Failed to create review id");
            return;
        }

        RiderReview review = new RiderReview();
        review.reviewId = reviewId;
        review.orderId = orderId;
        review.riderEmail = riderEmail;
        review.customerName = customerName;
        review.rating = rating;
        review.comment = comment;
        review.createdAt = System.currentTimeMillis();

        reviewsRef.child(reviewId).setValue(review)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(
                        e.getMessage() == null ? "Failed to save review" : e.getMessage()));
    }

    static void getReviewsForRider(String riderEmail, ResultCallback<List<RiderReview>> callback) {
        Query query = db().child(NODE_REVIEWS)
                .orderByChild("riderEmail")
                .equalTo(riderEmail);

        query.get()
                .addOnSuccessListener(snapshot -> {
                    List<RiderReview> reviews = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        RiderReview review = child.getValue(RiderReview.class);
                        if (review != null) {
                            reviews.add(review);
                        }
                    }
                    reviews.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
                    callback.onSuccess(reviews);
                })
                .addOnFailureListener(e -> callback.onError(
                        e.getMessage() == null ? "Failed to load reviews" : e.getMessage()));
    }

    static void getLatestDeliveredOrderForAnyRider(ResultCallback<RiderOrder> callback) {
        db().child(NODE_ORDERS).get()
                .addOnSuccessListener(snapshot -> {
                    List<RiderOrder> delivered = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        RiderOrder order = child.getValue(RiderOrder.class);
                        if (order != null && OrderStatusStore.STATUS_DELIVERED.equals(order.status)) {
                            delivered.add(order);
                        }
                    }
                    if (delivered.isEmpty()) {
                        callback.onSuccess(null);
                        return;
                    }
                    delivered.sort((a, b) -> Long.compare(b.deliveredAt, a.deliveredAt));
                    callback.onSuccess(delivered.get(0));
                })
                .addOnFailureListener(e -> callback.onError(
                        e.getMessage() == null ? "Failed to load delivered order" : e.getMessage()));
    }

    private static boolean isValidTransition(String current, String next) {
        if (current == null || current.trim().isEmpty()) {
            return OrderStatusStore.DEFAULT_STATUS.equals(next)
                    || OrderStatusStore.STATUS_PICKED_UP.equals(next);
        }
        if (current.equals(next)) {
            return true;
        }
        if (OrderStatusStore.DEFAULT_STATUS.equals(current)) {
            return OrderStatusStore.STATUS_PICKED_UP.equals(next);
        }
        if (OrderStatusStore.STATUS_PICKED_UP.equals(current)) {
            return OrderStatusStore.STATUS_ON_THE_WAY.equals(next);
        }
        if (OrderStatusStore.STATUS_ON_THE_WAY.equals(current)) {
            return OrderStatusStore.STATUS_DELIVERED.equals(next);
        }
        return false;
    }

    static void getOrderById(String orderId, ResultCallback<RiderOrder> callback) {
        db().child(NODE_ORDERS).child(orderId).get()
                .addOnSuccessListener(snapshot -> {
                    RiderOrder order = snapshot.getValue(RiderOrder.class);
                    callback.onSuccess(order);
                })
                .addOnFailureListener(e -> callback.onError(
                        e.getMessage() == null ? "Failed to load order" : e.getMessage()));
    }
}
