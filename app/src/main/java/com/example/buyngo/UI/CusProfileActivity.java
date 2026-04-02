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

public class CusProfileActivity extends AppCompatActivity {
    private TextView tvProfileName, tvFullName, tvEmail, tvPhoneNumber, tvAddress;
    private FirebaseAuth firebaseAuth;
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

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            firebaseAuth.signOut();
            Intent intent = new Intent(this, AuthWelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        loadUserProfile();
    }

    // Load profile data for logged-in user from Firebase
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
