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
        
        // Get orderId from intent
        orderId = getIntent().getStringExtra("orderId");
        Log.d(TAG, "Order ID: " + orderId);
        
        // Check if this is mandatory feedback (coming from order received)
        isMandatory = getIntent().getBooleanExtra("mandatory", false);
        Log.d(TAG, "Mandatory feedback: " + isMandatory);
        
        // If no orderId provided (coming from nav_reviews), show info message
        if (orderId == null || orderId.trim().isEmpty()) {
            ratingLabel.setText("Select an order from 'Orders' tab to add a review");
            findViewById(R.id.submitFeedbackButton).setEnabled(false);
            findViewById(R.id.submitFeedbackButton).setAlpha(0.5f);
            TextView skipTextView = findViewById(R.id.skipReview);
            if (skipTextView != null) {
                skipTextView.setVisibility(View.GONE);
            }
            return;
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
                ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
                    if (rating <= 0f) {
                        ratingLabel.setText("Tap a star to rate");
                        return;
                    }
                    ratingLabel.setText((int) rating + " / 5");
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up rating bar listener: " + e.getMessage());
        }

        try {
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

                // Use the orderId passed from intent
                if (orderId == null || orderId.trim().isEmpty()) {
                    Toast.makeText(CusFeedbackActivity.this,
                            "Order ID not provided",
                            Toast.LENGTH_SHORT).show();
                    navigateHome();
                    return;
                }

                // Get the order to find rider email
                FirebaseRiderRepository.getOrderById(orderId, 
                        new FirebaseRiderRepository.ResultCallback<FirebaseRiderRepository.RiderOrder>() {
                            @Override
                            public void onSuccess(FirebaseRiderRepository.RiderOrder order) {
                                if (order == null || order.assignedRiderEmail == null
                                        || order.assignedRiderEmail.trim().isEmpty()) {
                                    Toast.makeText(CusFeedbackActivity.this,
                                            "No rider assigned to this order",
                                            Toast.LENGTH_SHORT).show();
                                    navigateHome();
                                    return;
                                }

                                FirebaseRiderRepository.addReview(
                                        orderId,
                                        order.assignedRiderEmail,
                                        order.customerName == null ? "Customer" : order.customerName,
                                        rating,
                                        comment,
                                        new FirebaseRiderRepository.VoidCallback() {
                                            @Override
                                            public void onSuccess() {
                                                Toast.makeText(CusFeedbackActivity.this,
                                                        "Thanks for your feedback!",
                                                        Toast.LENGTH_SHORT).show();
                                                updateOrderAsReviewed(orderId);
                                                navigateHome();
                                            }

                                            @Override
                                            public void onError(String message) {
                                                Toast.makeText(CusFeedbackActivity.this,
                                                        message,
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(CusFeedbackActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        });
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up submit button: " + e.getMessage());
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
}