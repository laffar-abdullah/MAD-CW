package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.buyngo.Model.Review;
import com.example.buyngo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CusFeedbackActivity extends AppCompatActivity {

    private RatingBar ratingBar;
<<<<<<< HEAD
    private TextView ratingLabel;
    private EditText reviewComment;
=======
    private EditText reviewComment;
    private TextView ratingLabel;
    private DatabaseReference db;
    private FirebaseAuth mAuth;
    private String orderId;
>>>>>>> cc33148f16efd7cd1a6422a65c9b53b87be2e710

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_feedback);

        db = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        orderId = getIntent().getStringExtra("orderId");
        if (orderId == null) {
            orderId = "unknown-order";
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ratingBar = findViewById(R.id.ratingBar);
<<<<<<< HEAD
        ratingLabel = findViewById(R.id.ratingLabel);
        reviewComment = findViewById(R.id.reviewComment);

        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            if (rating <= 0f) {
                ratingLabel.setText("Tap a star to rate");
                return;
            }
            ratingLabel.setText((int) rating + " / 5");
        });

        findViewById(R.id.submitFeedbackButton).setOnClickListener(v -> {
            int rating = Math.round(ratingBar.getRating());
            String comment = reviewComment.getText().toString().trim();
            if (rating < 1) {
                Toast.makeText(this, "Please select a star rating", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseRiderRepository.getLatestDeliveredOrderForAnyRider(
                    new FirebaseRiderRepository.ResultCallback<FirebaseRiderRepository.RiderOrder>() {
                        @Override
                        public void onSuccess(FirebaseRiderRepository.RiderOrder order) {
                            if (order == null || order.assignedRiderEmail == null
                                    || order.assignedRiderEmail.trim().isEmpty()) {
                                Toast.makeText(CusFeedbackActivity.this,
                                        "No delivered order found to review",
                                        Toast.LENGTH_SHORT).show();
                                navigateHome();
                                return;
                            }

                            FirebaseRiderRepository.addReview(
                                    order.orderId,
                                    order.assignedRiderEmail,
                                    order.customerName == null ? "Customer" : order.customerName,
                                    rating,
                                    comment,
                                    new FirebaseRiderRepository.VoidCallback() {
                                        @Override
                                        public void onSuccess() {
                                            Toast.makeText(CusFeedbackActivity.this,
                                                    "Thanks for your feedback",
                                                    Toast.LENGTH_SHORT).show();
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

        findViewById(R.id.skipReview).setOnClickListener(v -> navigateHome());
    }

    private void navigateHome() {
=======
        reviewComment = findViewById(R.id.reviewComment);
        ratingLabel = findViewById(R.id.ratingLabel);

        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            if (rating == 1) ratingLabel.setText("Poor");
            else if (rating == 2) ratingLabel.setText("Fair");
            else if (rating == 3) ratingLabel.setText("Good");
            else if (rating == 4) ratingLabel.setText("Very Good");
            else if (rating == 5) ratingLabel.setText("Excellent!");
        });

        findViewById(R.id.submitFeedbackButton).setOnClickListener(v -> submitFeedback());
        findViewById(R.id.skipReview).setOnClickListener(v -> goHome());
    }

    // Submit review to Firebase database
    private void submitFeedback() {
        float rating = ratingBar.getRating();
        if (rating == 0) {
            Toast.makeText(this, "Please select a star rating", Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = reviewComment.getText().toString().trim();

        String userId = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid()
                : "anonymous";

        String userEmail = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getEmail()
                : "unknown@email.com";

        Review review = new Review(orderId, userId, userEmail, rating, comment);

        db.child("reviews").push().setValue(review)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                    goHome();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Navigate back to home without saving review
    private void goHome() {
>>>>>>> cc33148f16efd7cd1a6422a65c9b53b87be2e710
        Intent intent = new Intent(this, CusHomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}