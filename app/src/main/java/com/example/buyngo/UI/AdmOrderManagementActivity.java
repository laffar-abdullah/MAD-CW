package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.buyngo.R;

public class AdmOrderManagementActivity extends AppCompatActivity {

    private static final String EXTRA_ORDER_ID = "extra_order_id";
    private static final String EXTRA_CUSTOMER_NAME = "extra_customer_name";
    private static final String EXTRA_CUSTOMER_ADDRESS = "extra_customer_address";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adm_order_management);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.btnAssignRider).setOnClickListener(v ->
            openAssignRider("BNG-001", "Alice Smith", "12 Main Street, Springfield"));

        findViewById(R.id.btnAssignRiderOrder2).setOnClickListener(v ->
            openAssignRider("BNG-002", "Bob Jones", "78 Hill Road, Brookfield"));

        findViewById(R.id.btnAssignRiderOrder3).setOnClickListener(v ->
            openAssignRider("BNG-003", "Carol Lee", "45 Lake Avenue, Maple Town"));
        }

        private void openAssignRider(String orderId, String customerName, String customerAddress) {
        Intent intent = new Intent(this, AdmAssignRiderActivity.class);
        intent.putExtra(EXTRA_ORDER_ID, orderId);
        intent.putExtra(EXTRA_CUSTOMER_NAME, customerName);
        intent.putExtra(EXTRA_CUSTOMER_ADDRESS, customerAddress);
        startActivity(intent);
    }
}