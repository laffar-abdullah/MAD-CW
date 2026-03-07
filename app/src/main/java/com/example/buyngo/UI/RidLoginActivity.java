package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;

public class RidLoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rid_login);

        // Back to Welcome screen
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Login -> Rider Dashboard (no validation for now)
        findViewById(R.id.riderLoginButton).setOnClickListener(v -> {
            startActivity(new Intent(this, RidDashboardActivity.class));
            finish();
        });
    }
}