ackage com.example.buyngo.UI;

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
    private FirebaseAuth firebaseAuth;                // Manages login/signup
    private FirebaseDatabase firebaseDatabase;       // Stores user profiles

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_signup);

        // Connect to Firebase services
        firebaseAuth = FirebaseAuth.getInstance();        // For authentication
        firebaseDatabase = FirebaseDatabase.getInstance(); // For user data

        // Bind all form fields from layout
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        addressEditText = findViewById(R.id.addressEditText);
        cityEditText = findViewById(R.id.cityEditText);
        signupButton = findViewById(R.id.signupButton);

        // When "Create Account" tapped, start signup process
        signupButton.setOnClickListener(v -> performSignup());

        // "Already have account?" link goes back to login
        findViewById(R.id.loginLink).setOnClickListener(v -> finish());
    }

    // When customer clicks "Create Account" button
    private void performSignup() {
        // Step 1: Get text from all form fields
        String fullName = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String city = cityEditText.getText().toString().trim();

        // Step 2: Check if name is filled, if not show error
        if (fullName.isEmpty()) {
            nameEditText.setError("Full name is required");
            return;
        }

        // Step 3: Check if email is filled, if not show error
        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            return;
        }

        // Step 4: Check if password is filled, if not show error
        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            return;
        }

        // Step 5: Check if password is at least 6 characters (Firebase requirement)
        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            return;
        }

        // Step 6: Check if phone is filled, if not show error
        if (phone.isEmpty()) {
            phoneEditText.setError("Phone number is required");
            return;
        }

        // Step 7: Check if address is filled, if not show error
        if (address.isEmpty()) {
            addressEditText.setError("Address is required");
            return;
        }

        // Step 8: Check if city is filled, if not show error
        if (city.isEmpty()) {
            cityEditText.setError("City is required");
            return;
        }

        // Step 9: Disable button and show "Creating Account..." while processing
        signupButton.setEnabled(false);
        signupButton.setText("Creating Account...");

        // Step 10: Send email and password to Firebase Auth to create account
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                // Step 11: If account created successfully
                .addOnSuccessListener(authResult -> {
                    // Get the newly created user from Firebase
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        // Step 12: Save customer profile details to Firebase database
                        saveUserProfile(firebaseUser.getUid(), fullName, email, phone, address, city);
                    }
                })
                // Step 13: If account creation failed (email already exists, etc)
                .addOnFailureListener(e -> {
                    // Re-enable button for user to try again
                    signupButton.setEnabled(true);
                    signupButton.setText("Create Account");
                    // Show error message why signup failed
                    Toast.makeText(this, "Signup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Step 12.1: Save all customer info to Firebase database under /users/{userId}/
    private void saveUserProfile(String userId, String fullName, String email, String phone, String address, String city) {
        // Create User object with all customer info
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
