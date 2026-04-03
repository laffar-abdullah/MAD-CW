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

    private static final String DB_URL = "https://buyngo-5b43e-default-rtdb.firebaseio.com/";

    private RatingBar ratingBar;
    private EditText reviewComment;
    private TextView ratingLabel;
    private DatabaseReference db;
    private FirebaseAuth mAuth;
    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_feedback);

        db    = FirebaseDatabase.getInstance(DB_URL).getReference();
        mAuth = FirebaseAuth.getInstance();

        orderId = getIntent().getStringExtra("orderId");
        if (orderId == null) orderId = "unknown-order";

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ratingBar     = findViewById(R.id.ratingBar);
        reviewComment = findViewById(R.id.reviewComment);
        ratingLabel   = findViewById(R.id.ratingLabel);

        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            if      (rating == 1) ratingLabel.setText("Poor");
            else if (rating == 2) ratingLabel.setText("Fair");
            else if (rating == 3) ratingLabel.setText("Good");
            else if (rating == 4) ratingLabel.setText("Very Good");
            else if (rating == 5) ratingLabel.setText("Excellent!");
        });

        findViewById(R.id.submitFeedbackButton).setOnClickListener(v -> submitFeedback());
        findViewById(R.id.skipReview).setOnClickListener(v -> goHome());
    }

    private void submitFeedback() {
        float rating = ratingBar.getRating();
        if (rating == 0) {
            Toast.makeText(this, "Please select a star rating", Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = reviewComment.getText().toString().trim();

        String userId = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid() : "anonymous";
        String userEmail = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getEmail() : "unknown@email.com";

        // ── Write to "feedbacks" node so AdmViewFeedbackActivity can read it ──
        Map<String, Object> feedback = new HashMap<>();
        feedback.put("orderId",   orderId);
        feedback.put("userId",    userId);
        feedback.put("userEmail", userEmail);
        feedback.put("rating",    rating);
        feedback.put("comment",   comment);
        feedback.put("timestamp", System.currentTimeMillis());
        feedback.put("flagged",   false);
        feedback.put("adminReply", "");

        db.child("feedbacks").push().setValue(feedback)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                    goHome();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void goHome() {
        Intent intent = new Intent(this, CusHomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}