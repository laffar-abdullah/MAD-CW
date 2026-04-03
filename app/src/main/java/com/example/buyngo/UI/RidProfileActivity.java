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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import com.example.buyngo.R;

/**
 * RidProfileActivity — shows the rider's profile details and lets them
 * log out or change their password directly in the Firebase Realtime Database.
 *
 * ── BUGS FIXED IN THIS VERSION ─────────────────────────────────────────────
 *
 *  BUG 1 — imgProfilePicture and btnChangePhoto were commented out in the
 *  original findViewById calls but used immediately after, causing an instant
 *  NullPointerException on every launch:
 *      // imgProfilePicture = findViewById(R.id.imgProfilePicture);  ← commented
 *      imgProfilePicture.setOnClickListener(...)                      ← NPE here
 *  FIX: uncommented both findViewByIds so the fields are properly initialised.
 *
 *  BUG 2 — btnChangePassword was bound with the WRONG layout ID:
 *      btnChangePassword = findViewById(R.id.btnChangePass);   ← old broken ID
 *  The layout has android:id="@+id/btnChangePassword" (fixed in rid_profile.xml).
 *  FIX: changed to R.id.btnChangePassword so it matches the layout exactly.
 *
 *  BUG 3 — showChangePasswordDialog() called FirebaseRiderRepository
 *  .changeRiderPassword(...) which is an unresolved class — it was never
 *  implemented anywhere in the project, so the build would fail.
 *  FIX: replaced with a direct Firebase Realtime Database query that:
 *    1. Finds the rider node whose email matches the logged-in rider.
 *    2. Checks that the "current password" the user typed matches the stored one.
 *    3. If it matches, writes the new password to riders/{key}/password.
 *  This keeps the logic self-contained with no extra repository class needed.
 *
 *  BUG 4 — navProfile click listener was launching a second RidProfileActivity.
 *  FIX: it now calls bindRiderData() to refresh in-place (carried from prev fix).
 * ───────────────────────────────────────────────────────────────────────────
 *
 * Firebase schema expected (Realtime Database):
 *   /riders/{pushKey}/
 *       name:     "James Rider"
 *       email:    "rider@buyngo.com"
 *       password: "rider123"
 *       phone:    "077 775 5668"
 *       vehicle:  "Motorbike"
 */
public class RidProfileActivity extends AppCompatActivity {

    // Used to launch the device photo picker for changing the profile picture.
    private ActivityResultLauncher<String> profileImagePicker;

    // Views
    private TextView  txtProfileNameHeader;
    private TextView  txtFullNameValue;
    private TextView  txtEmailValue;
    private TextView  txtPhoneValue;
    private TextView  txtVehicleValue;
    private TextView  txtTotalDeliveriesValue;
    private ImageView imgProfilePicture;
    private Button    btnChangePhoto;
    private Button    btnChangePassword;

    // Firebase Realtime Database root reference.
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Rider-only screen — send unauthenticated users to login.
        if (!RiderSessionStore.isLoggedIn(this)) {
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.rid_profile);

        // Initialise Firebase Realtime Database reference.
        dbRef = FirebaseDatabase.getInstance().getReference();

        // Register photo picker — result handled in handleProfileImageSelected().
        profileImagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::handleProfileImageSelected);

        // ── Toolbar ────────────────────────────────────────────────────────
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // ── View binding ───────────────────────────────────────────────────
        txtProfileNameHeader    = findViewById(R.id.txtProfileNameHeader);
        txtFullNameValue        = findViewById(R.id.txtFullNameValue);
        txtEmailValue           = findViewById(R.id.txtEmailValue);
        txtPhoneValue           = findViewById(R.id.txtPhoneValue);
        txtVehicleValue         = findViewById(R.id.txtVehicleValue);
        txtTotalDeliveriesValue = findViewById(R.id.txtTotalDeliveriesValue);

        // BUG 1 FIX: these two lines were commented out, causing NPE
        // the moment the next lines tried to call setOnClickListener on null.
        imgProfilePicture = findViewById(R.id.imgProfilePicture);
        btnChangePhoto    = findViewById(R.id.btnChangePhoto);

        // BUG 2 FIX: was R.id.btnChangePass — wrong ID, field stayed null,
        // click listener was never set, dialog never opened.
        btnChangePassword = findViewById(R.id.btnChangePassword);

        // Photo picker is launched by tapping either the avatar or the button.
        imgProfilePicture.setOnClickListener(v -> profileImagePicker.launch("image/*"));
        btnChangePhoto.setOnClickListener(v    -> profileImagePicker.launch("image/*"));

        // BUG 3 FIX: now calls our own showChangePasswordDialog() which talks
        // directly to Firebase Realtime Database — no missing repository class.
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // ── Bottom navigation ──────────────────────────────────────────────
        findViewById(R.id.navDashboard).setOnClickListener(v ->
                startActivity(new Intent(this, RidDashboardActivity.class)));

        findViewById(R.id.navHistory).setOnClickListener(v ->
                startActivity(new Intent(this, RidDeliveryHistoryActivity.class)));

        findViewById(R.id.navReviews).setOnClickListener(v ->
                startActivity(new Intent(this, RidReviewsActivity.class)));

        // BUG 4 FIX: was launching a duplicate RidProfileActivity.
        // Now just refreshes the displayed data in-place.
        findViewById(R.id.navProfile).setOnClickListener(v -> bindRiderData());

        // ── Populate card ──────────────────────────────────────────────────
        bindRiderData();

        // ── Logout ─────────────────────────────────────────────────────────
        // Clears SharedPrefs session and goes back to Welcome, clearing the
        // entire back stack so Back cannot return to a protected screen.
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            RiderSessionStore.clearSession(this);
            Intent intent = new Intent(this, AuthWelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    // ── Profile data ────────────────────────────────────────────────────────

    /**
     * Fills every profile TextView from the session store.
     * Delivery count is read from the rider-scoped history in OrderStatusStore.
     */
    private void bindRiderData() {
        RiderSessionStore.RiderProfile profile = RiderSessionStore.getCurrentRider(this);

        if (profile == null) {
            // Session was cleared unexpectedly — redirect to login.
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        txtProfileNameHeader.setText(profile.name);
        txtFullNameValue.setText(profile.name);
        txtEmailValue.setText(profile.email);
        txtPhoneValue.setText(profile.phone);
        txtVehicleValue.setText(profile.vehicle);

        // Load profile photo if a URL was saved; fall back to launcher icon.
        if (imgProfilePicture != null) {
            if (!TextUtils.isEmpty(profile.profileImageUrl)) {
                // Glide is declared in build.gradle — loads URL into ImageView.
                com.bumptech.glide.Glide.with(this)
                        .load(profile.profileImageUrl)
                        .placeholder(R.mipmap.ic_launcher_round)
                        .error(R.mipmap.ic_launcher_round)
                        .into(imgProfilePicture);
            } else {
                imgProfilePicture.setImageResource(R.mipmap.ic_launcher_round);
            }
        }

        // Delivery count — read from the local rider-scoped history.
        int completed = OrderStatusStore.getDeliveryHistory(this).size();
        txtTotalDeliveriesValue.setText(completed + " Deliveries");
    }

    // ── Photo picker ─────────────────────────────────────────────────────────

    /**
     * Called when the user selects an image from the device gallery.
     * For this build the image is displayed locally only — no Firebase Storage
     * upload is wired here since storage rules may not be configured yet.
     * Swap in your upload logic if Firebase Storage is available.
     */
    private void handleProfileImageSelected(Uri imageUri) {
        if (imageUri == null || imgProfilePicture == null) {
            return;
        }
        // Show the chosen image immediately using Glide.
        com.bumptech.glide.Glide.with(this)
                .load(imageUri)
                .placeholder(R.mipmap.ic_launcher_round)
                .into(imgProfilePicture);

        Toast.makeText(this, "Profile photo updated locally", Toast.LENGTH_SHORT).show();
    }

    // ── Change password dialog ───────────────────────────────────────────────

    /**
     * Shows a 3-field dialog (current password, new password, confirm).
     *
     * BUG 3 FIX — original code called FirebaseRiderRepository.changeRiderPassword()
     * which was never implemented in this project.  This method now does the
     * Firebase Realtime Database work directly:
     *
     *   1. Query /riders where email == logged-in rider's email.
     *   2. Read the stored password from the matching node.
     *   3. If it matches the "current password" the user typed → write new password.
     *   4. Show a success or error toast.
     *
     * Firebase schema: /riders/{pushKey}/{ email, password, name, phone, vehicle }
     */
    private void showChangePasswordDialog() {
        RiderSessionStore.RiderProfile profile = RiderSessionStore.getCurrentRider(this);
        if (profile == null) {
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        // ── Build the dialog UI programmatically ───────────────────────────
        int dp16 = (int) (16 * getResources().getDisplayMetrics().density);
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(dp16, dp16, dp16, dp16);

        EditText etCurrent = new EditText(this);
        etCurrent.setHint("Current password");
        etCurrent.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(etCurrent);

        EditText etNew = new EditText(this);
        etNew.setHint("New password");
        etNew.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(etNew);

        EditText etConfirm = new EditText(this);
        etConfirm.setHint("Confirm new password");
        etConfirm.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(etConfirm);

        // ── Show dialog ────────────────────────────────────────────────────
        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(container)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Save", (d, w) -> {
                    String current = etCurrent.getText().toString().trim();
                    String next    = etNew.getText().toString();
                    String confirm = etConfirm.getText().toString();

                    // ── Client-side validation ─────────────────────────────
                    if (TextUtils.isEmpty(current)
                            || TextUtils.isEmpty(next)
                            || TextUtils.isEmpty(confirm)) {
                        Toast.makeText(this,
                                "Fill in all password fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!next.equals(confirm)) {
                        Toast.makeText(this,
                                "New passwords do not match", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (next.length() < 6) {
                        Toast.makeText(this,
                                "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // ── Firebase Realtime Database password change ─────────
                    // Query the /riders node for the entry whose email matches
                    // the currently logged-in rider.
                    changePasswordInFirebase(profile.email, current, next);
                })
                .show();
    }

    /**
     * Queries /riders, finds the node matching {@code riderEmail}, verifies
     * {@code currentPassword} against the stored value, then writes
     * {@code newPassword} if the check passes.
     *
     * @param riderEmail      email of the logged-in rider (used as the lookup key)
     * @param currentPassword what the rider typed as their existing password
     * @param newPassword     the new password to write into the database
     */
    private void changePasswordInFirebase(
            String riderEmail,
            String currentPassword,
            String newPassword) {

        // Query: SELECT * FROM riders WHERE email = riderEmail
        Query query = dbRef.child("riders")
                .orderByChild("email")
                .equalTo(riderEmail);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    // No rider found for this email — should not happen if
                    // session is valid, but guard anyway.
                    Toast.makeText(RidProfileActivity.this,
                            "Rider account not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                // There should be exactly one match; iterate to get the key.
                for (DataSnapshot riderSnap : snapshot.getChildren()) {

                    // Read the password currently stored in the database.
                    String storedPassword =
                            riderSnap.child("password").getValue(String.class);

                    // ── Verify current password ────────────────────────────
                    if (storedPassword == null || !storedPassword.equals(currentPassword)) {
                        Toast.makeText(RidProfileActivity.this,
                                "Current password is incorrect",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // ── Write new password to /riders/{key}/password ───────
                    // Using setValue on just the password child so no other
                    // rider fields are overwritten.
                    riderSnap.getRef().child("password")
                            .setValue(newPassword)
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(RidProfileActivity.this,
                                            "Password updated successfully",
                                            Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(RidProfileActivity.this,
                                            "Failed to update password: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show());

                    // Only process the first (and only) matching node.
                    break;
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Firebase read was denied or the device is offline.
                Toast.makeText(RidProfileActivity.this,
                        "Database error: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}