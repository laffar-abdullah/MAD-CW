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

        // Navigate back to Login
        findViewById(R.id.loginLink).setOnClickListener(v -> {
            finish();
        });

        // Navigate to Home after signup
        findViewById(R.id.signupButton).setOnClickListener(v -> {
            startActivity(new Intent(this, CusHomeActivity.class));
        });
    }
}