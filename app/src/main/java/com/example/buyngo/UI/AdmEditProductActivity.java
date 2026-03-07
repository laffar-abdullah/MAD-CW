package com.example.buyngo.UI;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.buyngo.R;

public class AdmEditProductActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adm_edit_product);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Save Changes -> go back (demo)
        findViewById(R.id.btnSaveEdit).setOnClickListener(v -> finish());
        // Cancel -> go back
        findViewById(R.id.btnCancelEdit).setOnClickListener(v -> finish());
    }
}
