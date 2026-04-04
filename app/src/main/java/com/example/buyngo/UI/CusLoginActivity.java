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

\npublic class CusLoginActivity extends AppCompatActivity {
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

    
    private void performLogin() {
        // STEP 1: Get email from form field
        String email = emailEditText.getText().toString().trim();
        // STEP 2: Get password from form field
        String password = passwordEditText.getText().toString().trim();

        // STEP 3: Check if email is empty
        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            return;
        }

        // STEP 4: Check if password is empty
        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            return;
        }

        // STEP 5: Disable login button to prevent double-click and show "Logging in..." text
        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");

        // STEP 6: Send email and password to Firebase Auth for verification
        firebaseAuth.signInWithEmailAndPassword(email, password)
                // STEP 7: If email/password correct, Firebase returns success
                .addOnSuccessListener(authResult -> {
                    // STEP 8: Get current logged-in user from Firebase
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    // STEP 9: Check if user object exists
                    if (user != null) {
                        // STEP 10: Show success message to customer
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                        // STEP 11: Go to home screen where customer sees products
                        startActivity(new Intent(this, CusHomeActivity.class));
                        // STEP 12: Finish login screen so customer cannot go back
                        finish();
                    }
                })
                // STEP 13: If email/password incorrect, Firebase returns failure with error message
                .addOnFailureListener(e -> {
                    // STEP 14: Re-enable login button so customer can try again
                    loginButton.setEnabled(true);
                    // STEP 15: Change button text back to "Login"
                    loginButton.setText("Login");
                    // STEP 16: Show error message (e.g., "Invalid email or password")
                    Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

