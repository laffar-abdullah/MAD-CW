ackage com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.buyngo.R;

import java.text.DateFormat;
import java.util.List;


public class RidReviewsActivity extends AppCompatActivity {

    private LinearLayout reviewsContainer;
    private TextView txtNoReviews;
    private static final String TAG = "RidReviews";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep the Reviews screen within the authenticated rider experience.
        if (!RiderSessionStore.isLoggedIn(this)) {
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.rid_reviews);

        reviewsContainer = findViewById(R.id.reviewsContainer);
        txtNoReviews = findViewById(R.id.txtNoReviews);

        // ── IMPROVEMENT: register toolbar as the ActionBar ─────────────────
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Back arrow returns to whichever screen launched Reviews.
        toolbar.setNavigationOnClickListener(v -> finish());

        // ── Bottom navigation ──────────────────────────────────────────────
        findViewById(R.id.navDashboard).setOnClickListener(v ->
                startActivity(new Intent(this, RidDashboardActivity.class)));

        findViewById(R.id.navHistory).setOnClickListener(v ->
                startActivity(new Intent(this, RidDeliveryHistoryActivity.class)));

        // BUG FIX: was launching another RidReviewsActivity while already on
        // this screen.  Now does nothing — rider is already here.
        findViewById(R.id.navReviews).setOnClickListener(v -> { /* already here */ });

        findViewById(R.id.navProfile).setOnClickListener(v ->
                startActivity(new Intent(this, RidProfileActivity.class)));

        renderReviews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderReviews();
    }

    private void renderReviews() {
        RiderSessionStore.RiderProfile profile = RiderSessionStore.getCurrentRider(this);
        if (profile == null) {
            Log.w(TAG, "Profile is null - redirecting to login");
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        Log.d(TAG, "===== LOADING REVIEWS =====");
        Log.d(TAG, "Rider Name: " + profile.name);
        Log.d(TAG, "Rider Email: " + profile.email);
        Log.d(TAG, "Querying reviews for rider email: '" + profile.email + "'");

        // Using fallback method (does NOT require Firebase index)
        // Loads all reviews and filters client-side by riderEmail
        FirebaseRiderRepository.getReviewsForRiderFallback(
                profile.email,
                new FirebaseRiderRepository.ResultCallback<List<FirebaseRiderRepository.RiderReview>>() {
                    @Override
                    public void onSuccess(List<FirebaseRiderRepository.RiderReview> reviews) {
                        Log.d(TAG, "✓ Query succeeded. Found " + reviews.size() + " reviews");
                        displayReviews(reviews);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "✗ Query error: " + message);
                        reviewsContainer.removeAllViews();
                        txtNoReviews.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void displayReviews(List<FirebaseRiderRepository.RiderReview> reviews) {
        Log.d(TAG, "===== DISPLAYING REVIEWS =====");
        Log.d(TAG, "Total reviews to display: " + reviews.size());
        reviewsContainer.removeAllViews();

        if (reviews.isEmpty()) {
            Log.d(TAG, "No reviews found for rider");
            txtNoReviews.setVisibility(View.VISIBLE);
            return;
        }

        Log.d(TAG, "Displaying " + reviews.size() + " reviews:");
        txtNoReviews.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(RidReviewsActivity.this);
        DateFormat dateFormat = android.text.format.DateFormat
                .getMediumDateFormat(RidReviewsActivity.this);

        for (FirebaseRiderRepository.RiderReview review : reviews) {
            Log.d(TAG, "  - Order #" + review.orderId + " | " + review.rating + "⭐ | " + review.customerName);

            View card = inflater.inflate(
                    R.layout.item_rider_review,
                    reviewsContainer,
                    false);

            TextView txtOrderId = card.findViewById(R.id.txtReviewOrderId);
            TextView txtRating = card.findViewById(R.id.txtReviewRating);
            TextView txtCustomer = card.findViewById(R.id.txtReviewCustomer);
            TextView txtComment = card.findViewById(R.id.txtReviewComment);
            TextView txtDate = card.findViewById(R.id.txtReviewDate);

            txtOrderId.setText("Order #" + review.orderId);
            txtRating.setText(review.rating + " stars");
            txtCustomer.setText("Customer: " + review.customerName);
            txtComment.setText(
                    review.comment == null || review.comment.trim().isEmpty()
                            ? "No written comment"
                            : review.comment);
            txtDate.setText(dateFormat.format(review.createdAt));

            reviewsContainer.addView(card);
        }
    }
}
