package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;

public class CusSignupActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_signup);

        // This link takes the user back to the login screen if they already have an account.
        findViewById(R.id.loginLink).setOnClickListener(v -> {
            finish();
        });

        // After sign-up, the customer goes straight into the home screen.
        findViewById(R.id.signupButton).setOnClickListener(v -> {
            startActivity(new Intent(this, CusHomeActivity.class));
        });
    }
}