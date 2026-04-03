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

public class CusFeedbackActivity extends AppCompatActivity {

    private RatingBar ratingBar;
    private TextView ratingLabel;
    private EditText reviewComment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_feedback);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ratingBar = findViewById(R.id.ratingBar);
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
        Intent intent = new Intent(this, CusHomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}