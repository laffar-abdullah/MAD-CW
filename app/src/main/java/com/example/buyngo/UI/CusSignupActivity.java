package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.buyngo.Model.User;
import com.example.buyngo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class CusSignupActivity extends AppCompatActivity {
    private EditText nameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText phoneEditText;
    private EditText addressEditText;
    private EditText cityEditText;
    private Button signupButton;
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_signup);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        addressEditText = findViewById(R.id.addressEditText);
        cityEditText = findViewById(R.id.cityEditText);
        signupButton = findViewById(R.id.signupButton);

        signupButton.setOnClickListener(v -> performSignup());

        findViewById(R.id.loginLink).setOnClickListener(v -> finish());
    }

    // Validate input and create account in Firebase
    private void performSignup() {
        String fullName = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String city = cityEditText.getText().toString().trim();

        if (fullName.isEmpty()) {
            nameEditText.setError("Full name is required");
            return;
        }

        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            return;
        }

        if (phone.isEmpty()) {
            phoneEditText.setError("Phone number is required");
            return;
        }

        if (address.isEmpty()) {
            addressEditText.setError("Address is required");
            return;
        }

        if (city.isEmpty()) {
            cityEditText.setError("City is required");
            return;
        }

        signupButton.setEnabled(false);
        signupButton.setText("Creating Account...");

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        saveUserProfile(firebaseUser.getUid(), fullName, email, phone, address, city);
                    }
                })
                .addOnFailureListener(e -> {
                    signupButton.setEnabled(true);
                    signupButton.setText("Create Account");
                    Toast.makeText(this, "Signup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Save user profile data to Firebase database
    private void saveUserProfile(String userId, String fullName, String email, String phone, String address, String city) {
        User user = new User(
                userId,
                email,
                fullName,
                phone,
                address,
                city,
                System.currentTimeMillis()
        );

        firebaseDatabase.getReference("users")
                .child(userId)
                .setValue(user)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, CusHomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    signupButton.setEnabled(true);
                    signupButton.setText("Create Account");
                    Toast.makeText(this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}