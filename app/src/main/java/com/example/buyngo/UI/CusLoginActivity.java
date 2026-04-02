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

        // The back button returns the customer to the role-selection welcome screen.
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Login takes the customer into the main shopping home page.
        findViewById(R.id.loginButton).setOnClickListener(v ->
                startActivity(new Intent(this, CusHomeActivity.class)));

        // The sign-up link opens the account creation screen.
        findViewById(R.id.signupLink).setOnClickListener(v ->
                startActivity(new Intent(this, CusSignupActivity.class)));
    }
}