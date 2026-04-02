package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.buyngo.R;

public class RidReviewsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep reviews within authenticated rider experience.
        if (!RiderSessionStore.isLoggedIn(this)) {
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.rid_reviews);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Bottom navigation shortcuts
        findViewById(R.id.navDashboard).setOnClickListener(v ->
            startActivity(new Intent(this, RidDashboardActivity.class)));
        findViewById(R.id.navHistory).setOnClickListener(v ->
            startActivity(new Intent(this, RidDeliveryHistoryActivity.class)));
        findViewById(R.id.navProfile).setOnClickListener(v ->
            startActivity(new Intent(this, RidProfileActivity.class)));
    }
}
