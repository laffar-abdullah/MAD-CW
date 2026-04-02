package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.buyngo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class CusFeedbackActivity extends AppCompatActivity {

    // These fields keep the feedback form connected to the screen.
    private RatingBar ratingBar;
    private EditText reviewComment;
    private TextView ratingLabel;

    // Firebase Realtime Database stores submitted customer reviews.
    private DatabaseReference db;

    // FirebaseAuth helps us tag the feedback with the signed-in customer.
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_feedback);

        // Connect Firebase once when the feedback screen opens.
        db = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        // The toolbar back arrow closes this feedback page.
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Read the star rating, review box, and label from the layout.
        ratingBar = findViewById(R.id.ratingBar);
        reviewComment = findViewById(R.id.reviewComment);
        ratingLabel = findViewById(R.id.ratingLabel);

        // Turn the star value into a friendly word so the page feels more human.
        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            if (rating == 1) ratingLabel.setText("Poor");
            else if (rating == 2) ratingLabel.setText("Fair");
            else if (rating == 3) ratingLabel.setText("Good");
            else if (rating == 4) ratingLabel.setText("Very Good");
            else if (rating == 5) ratingLabel.setText("Excellent!");
        });

        // Send the review to Firebase when the user taps submit.
        findViewById(R.id.submitFeedbackButton).setOnClickListener(v -> submitFeedback());

        // Skip returns the customer home without saving a review.
        findViewById(R.id.skipReview).setOnClickListener(v -> goHome());
    }

    private void submitFeedback() {
        // Collect the current rating and the written review.
        float rating = ratingBar.getRating();
        String comment = reviewComment.getText().toString().trim();

        // A review without stars is not useful.
        if (rating == 0) {
            Toast.makeText(this, "Please select a star rating", Toast.LENGTH_SHORT).show();
            return;
        }

        // If nobody is signed in, keep the feedback anonymous.
        String userId = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid()
                : "anonymous";

        // Keep a readable email when available.
        String userEmail = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getEmail()
                : "Unknown";

        // Build the feedback record before saving it.
        Map<String, Object> feedback = new HashMap<>();
        feedback.put("userId", userId);
        feedback.put("userEmail", userEmail);
        feedback.put("rating", rating);
        feedback.put("comment", comment);
        feedback.put("timestamp", System.currentTimeMillis());

        // Push the review into the shared feedback node.
        db.child("feedbacks").push().setValue(feedback)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                    goHome();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to submit: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void goHome() {
        // Clear the stack so the customer returns cleanly to the home page.
        Intent intent = new Intent(this, CusHomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}