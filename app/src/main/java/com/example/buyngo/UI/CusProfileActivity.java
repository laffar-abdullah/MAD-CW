package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.buyngo.Model.User;
import com.example.buyngo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 *                       CUSTOMER PROFILE ACTIVITY
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * WHAT THIS SCREEN DOES:\n * Shows logged-in customer's profile information (name, email, phone, address).\n * Also has Logout button to exit account.\n * \n * HOW IT CONNECTS TO FIREBASE:\n * 1. When screen opens, loadUserProfile() is called\n * 2. Gets logged-in customer ID from Firebase Auth\n * 3. Reads user profile data from /users/{userId}/ in Firebase database\n * 4. Displays name, email, phone, address\n * 5. Logout button calls firebaseAuth.signOut() then goes to login screen\n * \n * DATA FLOW:\n * Firebase Auth (get customer ID) → Read from /users/{userId}/ → Display info\n * \n * IMPORTANT:\n * - This is READ-ONLY (customer cannot edit profile in this version)\n * - Profile data saved during signup and used for checkout address\n * ═══════════════════════════════════════════════════════════════════════════════\n */\npublic class CusProfileActivity extends AppCompatActivity {
    private TextView tvProfileName, tvFullName, tvEmail, tvPhoneNumber, tvAddress;
    // Get logged-in customer ID
    private FirebaseAuth firebaseAuth;
    // Read customer's profile data
    private FirebaseDatabase firebaseDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_profile);

        // Initialize Firebase Auth and Database
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        tvProfileName = findViewById(R.id.tvProfileName);
        tvFullName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvAddress = findViewById(R.id.tvAddress);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Logout button - sign out from Firebase and go back to login
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            firebaseAuth.signOut();
            Intent intent = new Intent(this, AuthWelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Load and display customer's profile info
        loadUserProfile();
    }

    /**
     * Read logged-in customer's profile from Firebase and display it
     */
    private void loadUserProfile() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = currentUser.getUid();

        tvProfileName.setText("Loading...");
        tvFullName.setText("Loading...");
        tvEmail.setText("Loading...");
        tvPhoneNumber.setText("Loading...");
        tvAddress.setText("Loading...");

        firebaseDatabase.getReference("users").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            try {
                                User user = snapshot.getValue(User.class);

                                if (user != null) {
                                    String fullName = user.getFullName();
                                    tvProfileName.setText(fullName);
                                    tvFullName.setText(fullName);
                                    tvEmail.setText(user.getEmail());
                                    tvPhoneNumber.setText(user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()
                                            ? user.getPhoneNumber() : "Not set");

                                    String address = user.getAddress();
                                    String city = user.getCity();
                                    String fullAddress = "";

                                    if (address != null && !address.isEmpty()) {
                                        fullAddress = address;
                                    }
                                    if (city != null && !city.isEmpty()) {
                                        fullAddress = fullAddress.isEmpty() ? city : fullAddress + ", " + city;
                                    }

                                    tvAddress.setText(!fullAddress.isEmpty() ? fullAddress : "Not set");
                                }
                            } catch (Exception e) {
                                Toast.makeText(CusProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(CusProfileActivity.this, "Profile not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(CusProfileActivity.this, "Failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
