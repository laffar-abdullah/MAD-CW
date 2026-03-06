package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;

public class CusLoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_login);

        // Back to Welcome screen
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Login -> Customer Home
        findViewById(R.id.loginButton).setOnClickListener(v ->
                startActivity(new Intent(this, CusHomeActivity.class)));

        // Sign Up link -> Signup screen
        findViewById(R.id.signupLink).setOnClickListener(v ->
                startActivity(new Intent(this, CusSignupActivity.class)));
    }
}