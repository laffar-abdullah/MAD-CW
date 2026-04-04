package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.buyngo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 *                       CUSTOMER LOGIN ACTIVITY
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * WHAT THIS SCREEN DOES:
 * Existing customers log in with email and password.
 * \n * HOW IT CONNECTS TO FIREBASE:\n * 1. Customer enters email and password\n * 2. performLogin() sends email/password to Firebase Auth\n * 3. Firebase verifies credentials (email/password must match signup)\n * 4. If successful, customer logged in\n * 5. Navigate to home screen (products list)\n * \n * DATA FLOW:\n * Email/Password Form → Firebase Auth verification → Success → CusHomeActivity\n * \n * IMPORTANT:\n * - Firebase Auth handles password security (never sent in plain text)\n * - Customer ID (UID) stored by Firebase Auth\n * - Other profile data stored in /users/{userId}/ collection\n * ═══════════════════════════════════════════════════════════════════════════════\n */\npublic class CusLoginActivity extends AppCompatActivity {
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    // Firebase Auth handles email/password verification
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_login);

        firebaseAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loginButton.setOnClickListener(v -> performLogin());

        // "Don't have account?" link goes to signup
        findViewById(R.id.signupLink).setOnClickListener(v ->
                startActivity(new Intent(this, CusSignupActivity.class)));
    }

    /**
     * Validate email/password and verify with Firebase Auth
     */
    private void performLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, CusHomeActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    loginButton.setEnabled(true);
                    loginButton.setText("Login");
                    Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}