package com.example.buyngo.UI;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import com.example.buyngo.R;

import java.util.List;

/**
 * Shows rider profile with total deliveries and average reviews
 */
public class RidProfileActivity extends AppCompatActivity {

    // ── Views — all IDs must match rid_profile.xml exactly ──────────────────
    private TextView  txtProfileNameHeader;
    private TextView  txtFullNameValue;
    private TextView  txtEmailValue;
    private TextView  txtPhoneValue;
    private TextView  txtVehicleValue;
    private TextView  txtTotalDeliveriesValue;   // shows "N Deliveries"
    private TextView  txtReviewsSummaryValue;    // shows "4.5 ★"
    private ImageView imgProfilePicture;

    // ── Firebase Realtime Database ───────────────────────────────────────────
    // Used only for the password-change flow (reads /riders node directly).
    private DatabaseReference dbRef;

    // ────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Session guard — unauthenticated users go to login immediately.
        if (!RiderSessionStore.isLoggedIn(this)) {
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.rid_profile);

    private static final String TAG = "RidProfile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Session guard — unauthenticated users go to login immediately.
        if (!RiderSessionStore.isLoggedIn(this)) {
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.rid_profile);

        // Initialize Firebase
        dbRef = FirebaseDatabase.getInstance().getReference();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Bind views
        txtProfileNameHeader   = findViewById(R.id.txtProfileNameHeader);
        txtFullNameValue       = findViewById(R.id.txtFullNameValue);
        txtEmailValue          = findViewById(R.id.txtEmailValue);
        txtPhoneValue          = findViewById(R.id.txtPhoneValue);
        txtVehicleValue        = findViewById(R.id.txtVehicleValue);
        txtTotalDeliveriesValue = findViewById(R.id.txtTotalDeliveriesValue);
        txtReviewsSummaryValue  = findViewById(R.id.txtReviewsSummaryValue);
        imgProfilePicture = findViewById(R.id.imgProfilePicture);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // Setup bottom navigation
        findViewById(R.id.navDashboard).setOnClickListener(v ->
                startActivity(new Intent(this, RidDashboardActivity.class)));
        findViewById(R.id.navHistory).setOnClickListener(v ->
                startActivity(new Intent(this, RidDeliveryHistoryActivity.class)));
        findViewById(R.id.navReviews).setOnClickListener(v ->
                startActivity(new Intent(this, RidReviewsActivity.class)));
        findViewById(R.id.navProfile).setOnClickListener(v -> bindRiderData());

        // Setup logout
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            RiderSessionStore.clearSession(this);
            Intent intent = new Intent(this, AuthWelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        bindRiderData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindRiderData();
    }

    // Load rider profile and stats
    private void bindRiderData() {
        RiderSessionStore.RiderProfile profile = RiderSessionStore.getCurrentRider(this);

        if (profile == null) {
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        txtProfileNameHeader.setText(profile.name);
        txtFullNameValue.setText(profile.name);
        txtEmailValue.setText(profile.email);
        txtPhoneValue.setText(profile.phone);
        txtVehicleValue.setText(profile.vehicle);

        txtTotalDeliveriesValue.setText("Loading…");
        txtReviewsSummaryValue.setText("Loading…");

        // Load profile image
        if (imgProfilePicture != null) {
            if (!TextUtils.isEmpty(profile.profileImageUrl)) {
                com.bumptech.glide.Glide.with(this)
                        .load(profile.profileImageUrl)
                        .placeholder(R.mipmap.ic_launcher_round)
                        .error(R.mipmap.ic_launcher_round)
                        .into(imgProfilePicture);
            } else {
                imgProfilePicture.setImageResource(R.mipmap.ic_launcher_round);
            }
        }

        loadDeliveryCount(profile.email);
        loadReviewsSummary(profile.email);
    }

    // Load total deliveries
    private void loadDeliveryCount(String riderEmail) {
        FirebaseRiderRepository.getDeliveredOrdersForRider(
                riderEmail,
                new FirebaseRiderRepository.ResultCallback<List<FirebaseRiderRepository.RiderOrder>>() {
                    @Override
                    public void onSuccess(List<FirebaseRiderRepository.RiderOrder> orders) {
                        int count = orders.size();
                        txtTotalDeliveriesValue.setText(count + " Deliveries");
                    }
                    @Override
                    public void onError(String message) {
                        txtTotalDeliveriesValue.setText("— Deliveries");
                    }
                });
    }

    // Load average reviews rating
    private void loadReviewsSummary(String riderEmail) {
        Log.d(TAG, "Loading reviews for: " + riderEmail);
        FirebaseRiderRepository.getReviewsForRiderFallback(
                riderEmail,
                new FirebaseRiderRepository.ResultCallback<List<FirebaseRiderRepository.RiderReview>>() {
                    @Override
                    public void onSuccess(List<FirebaseRiderRepository.RiderReview> reviews) {
                        if (reviews.isEmpty()) {
                            txtReviewsSummaryValue.setText("No reviews yet");
                            return;
                        }

                        double total = 0;
                        for (FirebaseRiderRepository.RiderReview review : reviews) {
                            total += review.rating;
                        }
                        double avg = total / reviews.size();
                        String summary = String.format(java.util.Locale.US, "%.1f ★", avg);
                        txtReviewsSummaryValue.setText(summary);
                    }
                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Failed to load reviews: " + message);
                        txtReviewsSummaryValue.setText("— reviews");
                    }
                });
    }

    // Show password change dialog
    private void showChangePasswordDialog() {
        RiderSessionStore.RiderProfile profile = RiderSessionStore.getCurrentRider(this);
        if (profile == null) {
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        int dp16 = (int) (16 * getResources().getDisplayMetrics().density);
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(dp16, dp16, dp16, dp16);

        EditText etCurrent = new EditText(this);
        etCurrent.setHint("Current password");
        etCurrent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(etCurrent);

        EditText etNew = new EditText(this);
        etNew.setHint("New password");
        etNew.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(etNew);

        EditText etConfirm = new EditText(this);
        etConfirm.setHint("Confirm new password");
        etConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(etConfirm);

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(container)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Save", (d, w) -> {
                    String current = etCurrent.getText().toString().trim();
                    String next    = etNew.getText().toString();
                    String confirm = etConfirm.getText().toString();

                    if (TextUtils.isEmpty(current) || TextUtils.isEmpty(next) || TextUtils.isEmpty(confirm)) {
                        Toast.makeText(this, "Fill in all password fields", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!next.equals(confirm)) {
                        Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (next.length() < 6) {
                        Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseRiderRepository.changeRiderPassword(
                            profile.email, current, next,
                            new FirebaseRiderRepository.ResultCallback<FirebaseRiderRepository.RiderAccount>() {
                                @Override
                                public void onSuccess(FirebaseRiderRepository.RiderAccount account) {
                                    Toast.makeText(RidProfileActivity.this,
                                            "Password updated successfully", Toast.LENGTH_SHORT).show();
                                }
                                @Override
                                public void onError(String message) {
                                    Toast.makeText(RidProfileActivity.this,
                                            message, Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .show();
    }
}