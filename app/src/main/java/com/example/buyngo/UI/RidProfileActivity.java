package com.example.buyngo.UI;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.buyngo.R;

import java.util.List;


public class RidProfileActivity extends AppCompatActivity {

    // ── Photo picker launcher ────────────────────────────────────────────────
    private ActivityResultLauncher<String> profileImagePicker;

    // References to layout views
    private TextView  txtProfileNameHeader;
    private TextView  txtFullNameValue;
    private TextView  txtEmailValue;
    private TextView  txtPhoneValue;
    private TextView  txtVehicleValue;
    private TextView  txtBirthdateValue;
    private TextView  txtTotalDeliveriesValue;
    private TextView  txtReviewsSummaryValue;
    private ImageView imgProfilePicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Session guard
        if (!RiderSessionStore.isLoggedIn(this)) {
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.rid_profile);

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
        txtBirthdateValue      = findViewById(R.id.txtBirthdateValue);
        txtTotalDeliveriesValue = findViewById(R.id.txtTotalDeliveriesValue);
        txtReviewsSummaryValue  = findViewById(R.id.txtReviewsSummaryValue);
        imgProfilePicture      = findViewById(R.id.imgProfilePicture);

        Button btnChangePassword = findViewById(R.id.btnChangePassword);

        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // ── Bottom navigation ─────────────────────────────────────────────────
        findViewById(R.id.navDashboard).setOnClickListener(v ->
                startActivity(new Intent(this, RidDashboardActivity.class)));

        findViewById(R.id.navHistory).setOnClickListener(v ->
                startActivity(new Intent(this, RidDeliveryHistoryActivity.class)));

        findViewById(R.id.navReviews).setOnClickListener(v ->
                startActivity(new Intent(this, RidReviewsActivity.class)));

        findViewById(R.id.navProfile).setOnClickListener(v -> bindRiderData());

        // ── Logout ────────────────────────────────────────────────────────────
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
        txtBirthdateValue.setText(!TextUtils.isEmpty(profile.birthdate) ? profile.birthdate : "Not set");

        txtTotalDeliveriesValue.setText("Loading…");
        txtReviewsSummaryValue.setText("Loading…");

        if (imgProfilePicture != null) {
            if (!TextUtils.isEmpty(profile.profileImageUrl)) {
                com.bumptech.glide.Glide.with(this)
                        .load(profile.profileImageUrl)
                        .placeholder(R.drawable.buyngologo)
                        .error(R.drawable.buyngologo)
                        .into(imgProfilePicture);
            } else {
                imgProfilePicture.setImageResource(R.drawable.buyngologo);
            }
        }

        loadDeliveryCount(profile.email);
        loadReviewsSummary(profile.email);
    }

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

    private void loadReviewsSummary(String riderEmail) {
        FirebaseRiderRepository.getReviewsForRider(
                riderEmail,
                new FirebaseRiderRepository.ResultCallback<List<FirebaseRiderRepository.RiderReview>>() {
                    @Override
                    public void onSuccess(List<FirebaseRiderRepository.RiderReview> reviews) {
                        if (reviews == null || reviews.isEmpty()) {
                            txtReviewsSummaryValue.setText("No reviews yet");
                            return;
                        }

                        double sum = 0;
                        for (FirebaseRiderRepository.RiderReview r : reviews) {
                            sum += r.rating;
                        }
                        double avg = sum / reviews.size();
                        txtReviewsSummaryValue.setText(
                                String.format(java.util.Locale.US, "%.1f ★ ", avg));
                    }

                    @Override
                    public void onError(String message) {
                        txtReviewsSummaryValue.setText(" ★");
                    }
                });
    }


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

        final EditText etCurrent = new EditText(this);
        etCurrent.setHint("Current Password");
        etCurrent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(etCurrent);

        final EditText etNew = new EditText(this);
        etNew.setHint("New Password");
        etNew.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(etNew);

        final EditText etConfirm = new EditText(this);
        etConfirm.setHint("Confirm New Password");
        etConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(etConfirm);

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    String current = etCurrent.getText().toString();
                    String newPass = etNew.getText().toString();
                    String confirm = etConfirm.getText().toString();

                    if (TextUtils.isEmpty(current) || TextUtils.isEmpty(newPass)) {
                        Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newPass.equals(confirm)) {
                        Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseRiderRepository.changeRiderPassword(
                            profile.email,
                            current,
                            newPass,
                            new FirebaseRiderRepository.ResultCallback<FirebaseRiderRepository.RiderAccount>() {
                                @Override
                                public void onSuccess(FirebaseRiderRepository.RiderAccount result) {
                                    Toast.makeText(RidProfileActivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(String message) {
                                    Toast.makeText(RidProfileActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
