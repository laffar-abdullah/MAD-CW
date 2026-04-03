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
import com.bumptech.glide.Glide;
import com.example.buyngo.R;

/**
 * RidProfileActivity — displays the logged-in rider's personal details and
 * total completed deliveries, and provides a logout button.
 *
 * Profile data is read from {@link RiderSessionStore} (name, email, phone,
 * vehicle) and the delivery count from {@link OrderStatusStore#getDeliveryHistory}.
 *
 * ── CHANGES FROM ORIGINAL ──────────────────────────────────────────────────
 *  BUG FIX — The original navProfile bottom-nav item (the active tab on this
 *  screen) had a click listener that launched a NEW RidProfileActivity on top
 *  of the current one, stacking duplicates on the back stack every time the
 *  rider tapped "Profile" while already on the profile screen.
 *  FIX: the navProfile listener now calls bindRiderData() to simply refresh
 *  the displayed data in-place instead of starting another activity.
 *
 *  IMPROVEMENT — Added a null-guard before accessing profile fields in
 *  bindRiderData() so the app does not crash if the session is cleared from
 *  another thread between the isLoggedIn check and the actual data read.
 * ───────────────────────────────────────────────────────────────────────────
 */
public class RidProfileActivity extends AppCompatActivity {

    private ActivityResultLauncher<String> profileImagePicker;

    private TextView txtProfileNameHeader;
    private TextView txtFullNameValue;
    private TextView txtEmailValue;
    private TextView txtPhoneValue;
    private TextView txtVehicleValue;
    private TextView txtTotalDeliveriesValue;
    private ImageView imgProfilePicture;
    private Button btnChangePhoto;
    private Button btnChangePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Profile is part of the rider-only flow — enforce an active session.
        if (!RiderSessionStore.isLoggedIn(this)) {
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.rid_profile);

        profileImagePicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handleProfileImageSelected);

        // ── Toolbar ────────────────────────────────────────────────────────
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Back arrow returns to whichever screen launched the profile.
        toolbar.setNavigationOnClickListener(v -> finish());

        // ── View binding ───────────────────────────────────────────────────
        txtProfileNameHeader    = findViewById(R.id.txtProfileNameHeader);
        txtFullNameValue        = findViewById(R.id.txtFullNameValue);
        txtEmailValue           = findViewById(R.id.txtEmailValue);
        txtPhoneValue           = findViewById(R.id.txtPhoneValue);
        txtVehicleValue         = findViewById(R.id.txtVehicleValue);
        txtTotalDeliveriesValue = findViewById(R.id.txtTotalDeliveriesValue);
//        imgProfilePicture       = findViewById(R.id.imgProfilePicture);
//        btnChangePhoto          = findViewById(R.id.btnChangePhoto);
        btnChangePassword       = findViewById(R.id.btnChangePass);

        imgProfilePicture.setOnClickListener(v -> profileImagePicker.launch("image/*"));
        btnChangePhoto.setOnClickListener(v -> profileImagePicker.launch("image/*"));
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // ── Bottom navigation ──────────────────────────────────────────────
        findViewById(R.id.navDashboard).setOnClickListener(v ->
                startActivity(new Intent(this, RidDashboardActivity.class)));

        findViewById(R.id.navHistory).setOnClickListener(v ->
                startActivity(new Intent(this, RidDeliveryHistoryActivity.class)));

        findViewById(R.id.navReviews).setOnClickListener(v ->
                startActivity(new Intent(this, RidReviewsActivity.class)));

        // BUG FIX: navProfile was launching a new RidProfileActivity and
        // stacking duplicates.  Now just refreshes the current screen.
        findViewById(R.id.navProfile).setOnClickListener(v -> bindRiderData());

        // ── Populate profile data ──────────────────────────────────────────
        bindRiderData();

        // ── Logout ─────────────────────────────────────────────────────────
        // Clears the session and sends the rider back to the Welcome screen,
        // clearing the entire back stack so they cannot press Back to return.
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            RiderSessionStore.clearSession(this);
            Intent intent = new Intent(this, AuthWelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    /**
     * Reads the rider's saved profile from the session store and fills in all
     * the text views on the card.
     *
     * The delivery count is pulled from the rider-scoped history list so it
     * matches exactly what is shown on the Delivery History screen.
     *
     * IMPROVEMENT: null-guard on profile prevents a NullPointerException in
     * the unlikely event the session is cleared between the isLoggedIn check
     * and this read.
     */
    private void bindRiderData() {
        RiderSessionStore.RiderProfile profile = RiderSessionStore.getCurrentRider(this);

        if (profile == null) {
            // Session was cleared unexpectedly — send to login.
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        txtProfileNameHeader.setText(profile.name);
        txtFullNameValue.setText(profile.name);
        txtEmailValue.setText(profile.email);
        txtPhoneValue.setText(profile.phone);
        txtVehicleValue.setText(profile.vehicle);

        if (!TextUtils.isEmpty(profile.profileImageUrl)) {
            Glide.with(this)
                    .load(profile.profileImageUrl)
                    .placeholder(R.mipmap.ic_launcher_round)
                    .error(R.mipmap.ic_launcher_round)
                    .into(imgProfilePicture);
        } else {
            imgProfilePicture.setImageResource(R.mipmap.ic_launcher_round);
        }

        FirebaseRiderRepository.getDeliveredOrdersForRider(
                profile.email,
                new FirebaseRiderRepository.ResultCallback<java.util.List<FirebaseRiderRepository.RiderOrder>>() {
                    @Override
                    public void onSuccess(java.util.List<FirebaseRiderRepository.RiderOrder> result) {
                        txtTotalDeliveriesValue.setText(result.size() + " Deliveries");
                    }

                    @Override
                    public void onError(String message) {
                        txtTotalDeliveriesValue.setText("0 Deliveries");
                    }
                });
    }

    private void handleProfileImageSelected(Uri imageUri) {
        if (imageUri == null) {
            return;
        }

        RiderSessionStore.RiderProfile profile = RiderSessionStore.getCurrentRider(this);
        if (profile == null) {
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        FirebaseRiderRepository.updateRiderProfileImage(
                profile.email,
                imageUri,
                new FirebaseRiderRepository.ResultCallback<FirebaseRiderRepository.RiderAccount>() {
                    @Override
                    public void onSuccess(FirebaseRiderRepository.RiderAccount account) {
                        String vehicleDisplay = account.vehicle == null ? profile.vehicle : account.vehicle;
                        if (account.vehicleNumber != null && !account.vehicleNumber.trim().isEmpty()) {
                            vehicleDisplay = vehicleDisplay + " - " + account.vehicleNumber;
                        }

                        RiderSessionStore.saveSession(
                                RidProfileActivity.this,
                                new RiderSessionStore.RiderProfile(
                                        account.name,
                                        account.email,
                                        account.phone,
                                        vehicleDisplay,
                                        account.profileImageUrl));
                        bindRiderData();
                        Toast.makeText(RidProfileActivity.this, "Profile picture updated", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(RidProfileActivity.this, message, Toast.LENGTH_SHORT).show();
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

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(padding, padding, padding, padding);

        EditText currentPassword = new EditText(this);
        currentPassword.setHint("Current password");
        currentPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(currentPassword);

        EditText newPassword = new EditText(this);
        newPassword.setHint("New password");
        newPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(newPassword);

        EditText confirmPassword = new EditText(this);
        confirmPassword.setHint("Confirm new password");
        confirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(confirmPassword);

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(container)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Save", (dialog, which) -> {
                    String current = currentPassword.getText().toString().trim();
                    String next = newPassword.getText().toString();
                    String confirm = confirmPassword.getText().toString();

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
                            profile.email,
                            current,
                            next,
                            new FirebaseRiderRepository.ResultCallback<FirebaseRiderRepository.RiderAccount>() {
                                @Override
                                public void onSuccess(FirebaseRiderRepository.RiderAccount account) {
                                    Toast.makeText(RidProfileActivity.this, "Password updated", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(String message) {
                                    Toast.makeText(RidProfileActivity.this, message, Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .show();
    }
}
