package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;

public class AdmLoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adm_login);

        // Back to Welcome screen
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Login Admin Dashboard
        findViewById(R.id.adminLoginButton).setOnClickListener(v ->
                startActivity(new Intent(this, AdmDashboardActivity.class)));
    }
}
