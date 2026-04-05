ackage com.example.buyngo.UI;

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


public class RidProfileActivity extends AppCompatActivity {

    // ── Photo picker launcher ────────────────────────────────────────────────
    // Registered in onCreate(); result handled in handleProfileImageSelected().
    private ActivityResultLauncher<String> profileImagePicker;

    private static final String TAG = "RidProfile";

    // ── Views — all IDs must match rid_profile.xml exactly ──────────────────
    private TextView  txtProfileNameHeader;
    private TextView  txtFullNameValue;
    private TextView  txtEmailValue;
    private TextView  txtPhoneValue;
    private TextView  txtVehicleValue;
    private TextView  txtTotalDeliveriesValue;   // shows "N Deliveries"
    private TextView  txtReviewsSummaryValue;    // shows "4.5 ★ (N reviews)"
    private ImageView imgProfilePicture;
    private Button    btnChangePhoto;
    private Button    btnChangePassword;

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

        // Initialise Firebase Realtime Database root reference (for password changes).
        dbRef = FirebaseDatabase.getInstance().getReference();

        // Register the photo picker — Android handles the gallery UI,
        // we just receive the selected URI in handleProfileImageSelected().
        profileImagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::handleProfileImageSelected);

        // ── Toolbar ──────────────────────────────────────────────────────────
        // setSupportActionBar wires up the Toolbar as the ActionBar so the
        // back arrow (homeAsUpIndicator in the XML) works correctly.
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // ── View binding ─────────────────────────────────────────────────────
        txtProfileNameHeader   = findViewById(R.id.txtProfileNameHeader);
        txtFullNameValue       = findViewById(R.id.txtFullNameValue);
        txtEmailValue          = findViewById(R.id.txtEmailValue);
        txtPhoneValue          = findViewById(R.id.txtPhoneValue);
        txtVehicleValue        = findViewById(R.id.txtVehicleValue);
        txtTotalDeliveriesValue = findViewById(R.id.txtTotalDeliveriesValue);
        txtReviewsSummaryValue  = findViewById(R.id.txtReviewsSummaryValue);

        // BUG 1 FIX: these were commented-out in the original, causing NPE.
        imgProfilePicture = findViewById(R.id.imgProfilePicture);
        btnChangePhoto    = findViewById(R.id.btnChangePhoto);

        // BUG 2 FIX: was R.id.btnChangePass (wrong ID), field stayed null.
        btnChangePassword = findViewById(R.id.btnChangePassword);

        // Both the avatar image and the "Change Photo" button open the picker.
        imgProfilePicture.setOnClickListener(v -> profileImagePicker.launch("image/*"));
        btnChangePhoto.setOnClickListener(v    -> profileImagePicker.launch("image/*"));

        // BUG 3 FIX: calls our own dialog which queries Firebase directly.
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // ── Bottom navigation ─────────────────────────────────────────────────
        // Each tab navigates to the corresponding screen, EXCEPT navProfile
        // which refreshes data in-place (BUG 4 FIX — was launching a duplicate).
        findViewById(R.id.navDashboard).setOnClickListener(v ->
                startActivity(new Intent(this, RidDashboardActivity.class)));

        findViewById(R.id.navHistory).setOnClickListener(v ->
                startActivity(new Intent(this, RidDeliveryHistoryActivity.class)));

        findViewById(R.id.navReviews).setOnClickListener(v ->
                startActivity(new Intent(this, RidReviewsActivity.class)));

        // BUG 4 FIX: was launching a duplicate RidProfileActivity; now refreshes.
        findViewById(R.id.navProfile).setOnClickListener(v -> bindRiderData());

        // ── Logout ────────────────────────────────────────────────────────────
        // Clears the SharedPrefs session then goes to Welcome, clearing the entire
        // back stack so the Back button cannot return to a protected screen.
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            RiderSessionStore.clearSession(this);
            Intent intent = new Intent(this, AuthWelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // ── Load all data ─────────────────────────────────────────────────────
        bindRiderData();
    }

    // ── onResume refreshes stats if the rider navigated back from Reviews/History
    @Override
    protected void onResume() {
        super.onResume();
        bindRiderData();
    }

    // ════════════════════════════════════════════════════════════════════════
    // bindRiderData()
    // ════════════════════════════════════════════════════════════════════════
    
    private void bindRiderData() {
        // ── Step 1: Fill basic profile from session cache (instant, no network) ──
        RiderSessionStore.RiderProfile profile = RiderSessionStore.getCurrentRider(this);

        if (profile == null) {
            // Session was unexpectedly cleared — redirect to login.
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        txtProfileNameHeader.setText(profile.name);
        txtFullNameValue.setText(profile.name);
        txtEmailValue.setText(profile.email);
        txtPhoneValue.setText(profile.phone);
        txtVehicleValue.setText(profile.vehicle);

        // Show a placeholder until Firebase returns.
        txtTotalDeliveriesValue.setText("Loading…");
        txtReviewsSummaryValue.setText("Loading…");

        // Load profile photo if a URL was saved; fall back to launcher icon.
        if (imgProfilePicture != null) {
            if (!TextUtils.isEmpty(profile.profileImageUrl)) {
                // Glide is declared in build.gradle — loads the URL asynchronously.
                com.bumptech.glide.Glide.with(this)
                        .load(profile.profileImageUrl)
                        .placeholder(R.mipmap.ic_launcher_round)
                        .error(R.mipmap.ic_launcher_round)
                        .into(imgProfilePicture);
            } else {
                imgProfilePicture.setImageResource(R.mipmap.ic_launcher_round);
            }
        }

        // ── Step 2: Delivery count from Firebase ─────────────────────────────
        // FirebaseRiderRepository.getDeliveredOrdersForRider() fetches all orders
        // from /orders, filters by assignedRiderEmail == profile.email AND
        // status == "Delivered", then returns the matching list.
        // We only need the count here, so we call .size().
        loadDeliveryCount(profile.email);

        // ── Step 3: Reviews summary from Firebase ────────────────────────────
        // FirebaseRiderRepository.getReviewsForRider() queries /reviews where
        // riderEmail == profile.email.  We compute the average rating here.
        loadReviewsSummary(profile.email);
    }

    // ════════════════════════════════════════════════════════════════════════
    // loadDeliveryCount()  — NEW 1
    // ════════════════════════════════════════════════════════════════════════
    
    private void loadDeliveryCount(String riderEmail) {
        FirebaseRiderRepository.getDeliveredOrdersForRider(
                riderEmail,
                new FirebaseRiderRepository.ResultCallback<List<FirebaseRiderRepository.RiderOrder>>() {

                    @Override
                    public void onSuccess(List<FirebaseRiderRepository.RiderOrder> orders) {
                        int count = orders.size();
                        // e.g. "12 Deliveries"
                        txtTotalDeliveriesValue.setText(count + " Deliveries");
                    }

                    @Override
                    public void onError(String message) {
                        // Network error — show dash so the screen is still readable.
                        txtTotalDeliveriesValue.setText("— Deliveries");
                    }
                });
    }

    // ════════════════════════════════════════════════════════════════════════
    // loadReviewsSummary()  — NEW 2
    // ════════════════════════════════════════════════════════════════════════
    
    private void loadReviewsSummary(String riderEmail) {
        Log.d(TAG, "Loading reviews summary for rider: " + riderEmail);
        
        // Using fallback method (does NOT require Firebase index)
        FirebaseRiderRepository.getReviewsForRiderFallback(
                riderEmail,
                new FirebaseRiderRepository.ResultCallback<List<FirebaseRiderRepository.RiderReview>>() {

                    @Override
                    public void onSuccess(List<FirebaseRiderRepository.RiderReview> reviews) {
                        Log.d(TAG, "✓ Reviews query succeeded. Found " + reviews.size() + " reviews");

                        if (reviews.isEmpty()) {
                            Log.d(TAG, "No reviews found for rider: " + riderEmail);
                            txtReviewsSummaryValue.setText("No reviews yet");
                            return;
                        }

                        // Compute average rating across all reviews.
                        double total = 0;
                        for (FirebaseRiderRepository.RiderReview review : reviews) {
                            Log.d(TAG, "  - Review: " + review.rating + "⭐ by " + review.customerName);
                            total += review.rating;
                        }
                        double avg = total / reviews.size();

                        // Format to one decimal place.  e.g. "4.5 ★  (8 reviews)"
                        String summary = String.format(
                                java.util.Locale.US,
                                "%.1f \u2605  (%d %s)",
                                avg,
                                reviews.size(),
                                reviews.size() == 1 ? "review" : "reviews");

                        Log.d(TAG, "Average rating: " + avg + " | Summary: " + summary);
                        txtReviewsSummaryValue.setText(summary);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "✗ Failed to load reviews: " + message);
                        // Network error — show dash so the screen is still readable.
                        txtReviewsSummaryValue.setText("— reviews");
                    }
                });
    }

    // ════════════════════════════════════════════════════════════════════════
    // Photo picker
    // ════════════════════════════════════════════════════════════════════════
    
    private void handleProfileImageSelected(Uri imageUri) {
        if (imageUri == null || imgProfilePicture == null) return;

        com.bumptech.glide.Glide.with(this)
                .load(imageUri)
                .placeholder(R.mipmap.ic_launcher_round)
                .into(imgProfilePicture);

        Toast.makeText(this, "Profile photo updated locally", Toast.LENGTH_SHORT).show();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Change password dialog  —  BUG 3 FIX
    // ════════════════════════════════════════════════════════════════════════
    
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

                    // Delegate to repository which reads /riders/{riderId}
                    // verifies currentPassword, then writes newPassword.
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
