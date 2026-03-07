package com.example.buyngo.UI;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;

public class RidStatusUpdateActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rid_status_update);

        findViewById(R.id.btnPickedUp).setOnClickListener(v -> { /* status UI only */ });
        findViewById(R.id.btnOutForDelivery).setOnClickListener(v -> { /* status UI only */ });
        findViewById(R.id.btnDelivered).setOnClickListener(v -> finish());
    }
}