package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;

public class CusTrackingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_tracking);

        // Go to Feedback screen after receiving order
        findViewById(R.id.receivedButton).setOnClickListener(v -> {
            startActivity(new Intent(this, CusFeedbackActivity.class));
        });
    }
}