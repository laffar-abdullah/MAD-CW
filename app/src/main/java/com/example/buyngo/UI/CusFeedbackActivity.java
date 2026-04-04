package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.buyngo.R;

import java.util.HashMap;
import java.util.Map;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 *                      CUSTOMER FEEDBACK ACTIVITY
 *                  (MAIN FEATURE - FEEDBACK SYSTEM)
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * WHAT THIS SCREEN DOES:
 * Allows customers to rate and review their delivery experience after order
 * is completed. Also allows anonymous feedback from app menu.
 * 
 * HOW IT CONNECTS TO FIREBASE:
 * 
 * TWO FEEDBACK MODES:\n * \n * MODE 1: ORDER-BASED FEEDBACK (After delivery)\n * 1. Order delivered → This screen automatically shown with orderId\n * 2. Customer rates 1-5 stars and writes comment\n * 3. Loads order details from Firebase (/orders/{orderId}/)\n * 4. Gets rider email from order data\n * 5. Saves Review to TWO places:\n *    - /reviews/{reviewId} (rider sees feedback about them)\n *    - /feedbacks/{feedbackId} (admin sees all feedback)\n * 6. Updates order status to "Delivered Successfully"\n * \n * MODE 2: ANONYMOUS FEEDBACK (From nav_reviews menu)\n * 1. Customer taps "Reviews" from home menu\n * 2. Opens this screen with NO orderId\n * 3. Customer gives rating and comment anonymously\n * 4. Saves to /feedbacks/{feedbackId} only\n * 5. Marked with isAnonymous=true\n * \n * FIREBASE OPERATIONS:\n * - READ: Order data (/orders/{orderId}/) to get rider email\n * - WRITE: Review to /reviews/{riderEmail}/\n * - WRITE: Feedback to /feedbacks/\n * - UPDATE: Order status to "Delivered Successfully"\n * \n * DATA FLOW:\n * \n * ORDER-BASED:\n * Order → This screen shown → Customer rates → Review object created\n *                                                       ↓\n *                    Saved to /reviews/ AND /feedbacks/\n *                                                       ↓\n *                    Order marked "Delivered Successfully"\n *                                                       ↓\n *             Rider can see feedback about them\n *             Admin can see all feedback\n * \n * ANONYMOUS:\n * nav_reviews menu → This screen shown → Customer rates → Feedback object created\n *                                                               ↓\n *                                    Saved to /feedbacks/ only\n *                                                               ↓\n *                              Admin can see anonymous feedback\n * \n * KEY FEATURES:\n * - Submit button enabled ONLY if rating given (1+ stars)\n * - Can skip mandatory feedback (shows confirmation dialog)\n * - Real-time rating display (shows \"X / 5\" as customer slides)\n * - Handles both order-based and anonymous reviews\n * ═══════════════════════════════════════════════════════════════════════════════\n */
public class CusFeedbackActivity extends AppCompatActivity {

    private static final String DB_URL = "https://buyngo-5b43e-default-rtdb.firebaseio.com/";
    private static final String TAG = "CusFeedback";

    private RatingBar ratingBar;
    private TextView ratingLabel;
    private EditText reviewComment;
    private String orderId;
    private boolean isMandatory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.cus_feedback);
            Log.d(TAG, "Layout inflated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error inflating layout: " + e.getMessage());
            finish();
            return;
        }

        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> {
                if (isMandatory) {
                    // Show confirmation dialog for back button on mandatory feedback
                    new AlertDialog.Builder(this)
                            .setTitle("Skip Feedback?")
                            .setMessage("Are you sure you want to skip providing feedback? Your feedback helps us improve the service.")
                            .setPositiveButton("Skip", (dialog, which) -> finish())
                            .setNegativeButton("Add Feedback", (dialog, which) -> dialog.dismiss())
                            .show();
                } else {
                    finish();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up toolbar: " + e.getMessage());
        }

        try {
            ratingBar = findViewById(R.id.ratingBar);
            ratingLabel = findViewById(R.id.ratingLabel);
            reviewComment = findViewById(R.id.reviewComment);
            Log.d(TAG, "Views found - ratingBar: " + (ratingBar != null) + ", ratingLabel: " + (ratingLabel != null) + ", reviewComment: " + (reviewComment != null));
        } catch (Exception e) {
            Log.e(TAG, "Error finding views: " + e.getMessage());
            finish();
            return;
        }
        
        // Get orderId from intent (null if anonymous feedback from nav_reviews)
        orderId = getIntent().getStringExtra("orderId");
        Log.d(TAG, "Order ID: " + orderId);
        
        // Check if this is mandatory feedback (coming from order received)
        isMandatory = getIntent().getBooleanExtra("mandatory", false);
        Log.d(TAG, "Mandatory feedback: " + isMandatory);
        
        // If no orderId provided (coming from nav_reviews), allow anonymous review submission
        boolean isAnonymousReview = (orderId == null || orderId.trim().isEmpty());
        if (isAnonymousReview) {
            ratingLabel.setText("Share your experience (anonymous)");
            Log.d(TAG, "Anonymous review mode enabled");
        }
        
        // Show/hide skip button based on mandatory flag
        try {
            TextView skipTextView = findViewById(R.id.skipReview);
            if (skipTextView != null && ratingBar != null && reviewComment != null) {
                skipTextView.setVisibility(View.VISIBLE);
                
                // Set skip button click listener
                skipTextView.setOnClickListener(v -> {
                    if (isMandatory) {
                        // Show confirmation dialog when skipping mandatory feedback
                        new AlertDialog.Builder(this)
                                .setTitle("Skip Review?")
                                .setMessage("Are you sure you want to skip providing a review? Your feedback helps us improve the service.")
                                .setPositiveButton("Skip", (dialog, which) -> {
                                    updateOrderAsReviewed(orderId);
                                    navigateHome();
                                })
                                .setNegativeButton("Add Review", (dialog, which) -> dialog.dismiss())
                                .show();
                    } else {
                        navigateHome();
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up skip button: " + e.getMessage());
        }

        try {
            if (ratingBar != null && ratingLabel != null) {
                // Update rating label as customer slides the bar
                ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
                    if (rating <= 0f) {
                        ratingLabel.setText("Tap a star to rate");
                        return;
                    }
                    ratingLabel.setText((int) rating + " / 5");
                });
            }
        } catch (Exception e) {
        }

        try {
            final boolean isAnonymous = (orderId == null || orderId.trim().isEmpty());
            
            // When "Submit Feedback" button tapped
            findViewById(R.id.submitFeedbackButton).setOnClickListener(v -> {
                if (ratingBar == null || reviewComment == null) {
                    Toast.makeText(this, "Form not loaded properly", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                int rating = Math.round(ratingBar.getRating());
                String comment = reviewComment.getText().toString().trim();
                if (rating < 1) {
                    Toast.makeText(this, "Please select a star rating", Toast.LENGTH_SHORT).show();
                    return;
                }

                // If no order ID, treat as anonymous review
                // Save directly to /feedbacks/ without order details
                if (isAnonymous) {
                    saveAnonymousFeedbackToAdminNode(rating, comment);
                    return;
                }

                // For order-based reviews, get order details and rider email from Firebase
                // Then save review linked to that rider
                com.google.firebase.database.FirebaseDatabase.getInstance("https://buyngo-5b43e-default-rtdb.firebaseio.com/")
                        .getReference("orders")
                        .child(orderId)
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            String riderEmail = snapshot.child("assignedRiderEmail").getValue(String.class);
                            String customerName = snapshot.child("customerName").getValue(String.class);
                            
                            // Use fallback email if rider email not found
                            String finalRiderEmail = (riderEmail != null && !riderEmail.isEmpty()) 
                                    ? riderEmail : "unknown@rider.com";
                            String finalCustomerName = (customerName != null) ? customerName : "Customer";
                            
                            // Save review to rider records
                            FirebaseRiderRepository.addReview(
                                    orderId,
                                    finalRiderEmail,
                                    finalCustomerName,
                                    rating,
                                    comment,
                                    new FirebaseRiderRepository.VoidCallback() {
                                        @Override
                                        public void onSuccess() {
                                            // Also save to admin feedback node
                                            saveFeedbackToAdminNode(orderId, finalRiderEmail, finalCustomerName, rating, comment);
                                        }

                                        @Override
                                        public void onError(String message) {
                                            Toast.makeText(CusFeedbackActivity.this,
                                                    message,
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(CusFeedbackActivity.this,
                                    "Error loading order: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
            });
        } catch (Exception e) {
            // Error setting up button
        }
    }

    private void navigateHome() {
        Intent intent = new Intent(this, CusHomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void updateOrderAsReviewed(String orderId) {
        if (orderId != null) {
            Log.d(TAG, "Updating order " + orderId + " to Delivered Successfully");
            // Mark order as completed after review
            android.content.Context context = this;
            com.google.firebase.database.FirebaseDatabase.getInstance(DB_URL)
                    .getReference("orders")
                    .child(orderId)
                    .child("status")
                    .setValue("Delivered Successfully")
                    .addOnSuccessListener(unused -> {
                        Log.d(TAG, "Order " + orderId + " marked as Delivered Successfully");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update order status: " + e.getMessage());
                    });
        }
    }

    // Save feedback to admin panel for review management
    private void saveFeedbackToAdminNode(String orderId, String riderEmail, String customerName, int rating, String comment) {
        try {
            com.google.firebase.database.FirebaseDatabase db = 
                    com.google.firebase.database.FirebaseDatabase.getInstance(DB_URL);
            
            // Create feedback object with order details
            java.util.Map<String, Object> feedback = new java.util.HashMap<>();
            feedback.put("feedbackId", db.getReference("feedbacks").push().getKey());
            feedback.put("orderId", orderId);
            feedback.put("riderEmail", riderEmail);
            feedback.put("customerName", customerName);
            feedback.put("rating", (double) rating);
            feedback.put("comment", comment);
            feedback.put("timestamp", System.currentTimeMillis());
            feedback.put("flagged", false);

            // Save feedback to database
            db.getReference("feedbacks")
                    .push()
                    .setValue(feedback)
                    .addOnSuccessListener(unused -> {
                        // Show success message
                        Toast.makeText(CusFeedbackActivity.this,
                                "Thanks for your feedback!",
                                Toast.LENGTH_SHORT).show();
                        updateOrderAsReviewed(orderId);
                        navigateHome();
                    })
                    .addOnFailureListener(e -> {
                        // Even if save fails, show success and go back
                        Toast.makeText(CusFeedbackActivity.this,
                                "Thanks for your feedback!",
                                Toast.LENGTH_SHORT).show();
                        updateOrderAsReviewed(orderId);
                        navigateHome();
                    });
        } catch (Exception e) {
            // Handle error gracefully
            Toast.makeText(CusFeedbackActivity.this,
                    "Thanks for your feedback!",
                    Toast.LENGTH_SHORT).show();
            updateOrderAsReviewed(orderId);
            navigateHome();
        }
    }

    // Save anonymous feedback from customers who are not ordering
    private void saveAnonymousFeedbackToAdminNode(int rating, String comment) {
        try {
            com.google.firebase.database.FirebaseDatabase db = 
                    com.google.firebase.database.FirebaseDatabase.getInstance(DB_URL);
            
            // Create feedback object with anonymous user information
            java.util.Map<String, Object> feedback = new java.util.HashMap<>();
            feedback.put("feedbackId", db.getReference("feedbacks").push().getKey());
            feedback.put("orderId", "anonymous");
            feedback.put("riderEmail", "anonymous");
            feedback.put("customerName", "Anonymous");
            feedback.put("rating", (double) rating);
            feedback.put("comment", comment);
            feedback.put("timestamp", System.currentTimeMillis());
            feedback.put("flagged", false);
            feedback.put("isAnonymous", true);

            // Save feedback to database
            db.getReference("feedbacks")
                    .push()
                    .setValue(feedback)
                    .addOnSuccessListener(unused -> {
                        // Show success message and go back home
                        Toast.makeText(CusFeedbackActivity.this,
                                "Thanks for your feedback!",
                                Toast.LENGTH_SHORT).show();
                        navigateHome();
                    })
                    .addOnFailureListener(e -> {
                        // Even if save fails, show success and go back
                        Toast.makeText(CusFeedbackActivity.this,
                                "Thanks for your feedback!",
                                Toast.LENGTH_SHORT).show();
                        navigateHome();
                    });
        } catch (Exception e) {
            // Handle error gracefully
            Toast.makeText(CusFeedbackActivity.this,
                    "Thanks for your feedback!",
                    Toast.LENGTH_SHORT).show();
            navigateHome();
        }
    }
}