package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.buyngo.R;

/**
 * RidReviewsActivity — shows customer feedback cards for this rider.
 *
 * In the current build the review cards are static (hard-coded in
 * rid_reviews.xml).  The screen is fully navigable from the bottom nav and
 * is protected by a session guard.
 *
 * ── CHANGES FROM ORIGINAL ──────────────────────────────────────────────────
 *  BUG FIX — The original navReviews bottom-nav listener (the active tab on
 *  this screen) launched a NEW RidReviewsActivity on top of the current one,
 *  stacking duplicates on the back stack every time the rider tapped
 *  "Reviews" while already viewing their reviews.
 *  FIX: the navReviews listener is now a no-op (does nothing) since the
 *  rider is already on this screen.  Tapping the active tab should never
 *  create a second copy of the same activity.
 *
 *  IMPROVEMENT — setSupportActionBar() is now called on the Toolbar so the
 *  system ActionBar is replaced properly and the up/back button works.
 * ───────────────────────────────────────────────────────────────────────────
 */
public class RidReviewsActivity extends AppCompatActivity {

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
    }
}
