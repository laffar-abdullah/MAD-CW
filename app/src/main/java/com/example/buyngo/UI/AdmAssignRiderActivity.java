package com.example.buyngo.UI;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.buyngo.R;

import java.util.List;

public class AdmAssignRiderActivity extends AppCompatActivity {

    private static final String EXTRA_ORDER_ID = "extra_order_id";
    private static final String EXTRA_CUSTOMER_NAME = "extra_customer_name";
    private static final String EXTRA_CUSTOMER_ADDRESS = "extra_customer_address";

    private LinearLayout riderListContainer;
    private View tvNoRiders;
    private Button btnConfirmAssign;
    private String selectedRiderEmail;
    private String orderId;
    private String customerName;
    private String customerAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adm_assign_rider);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        riderListContainer = findViewById(R.id.riderListContainer);
        tvNoRiders = findViewById(R.id.tvNoRiders);
        btnConfirmAssign = findViewById(R.id.btnConfirmAssign);

        orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        customerName = getIntent().getStringExtra(EXTRA_CUSTOMER_NAME);
        customerAddress = getIntent().getStringExtra(EXTRA_CUSTOMER_ADDRESS);

        if (orderId == null || orderId.trim().isEmpty()) {
            orderId = "BNG-001";
        }
        if (customerName == null || customerName.trim().isEmpty()) {
            customerName = "Alice Johnson";
        }
        if (customerAddress == null || customerAddress.trim().isEmpty()) {
            customerAddress = "12 Main Street, Springfield";
        }

        TextView tvOrderId = findViewById(R.id.tvOrderId);
        TextView tvOrderCustomer = findViewById(R.id.tvOrderCustomer);
        tvOrderId.setText("Order #" + orderId);
        tvOrderCustomer.setText("Customer: " + customerName);

        btnConfirmAssign.setOnClickListener(v -> assignOrder());

        loadRiders();
    }

    private void loadRiders() {
        riderListContainer.removeAllViews();
        selectedRiderEmail = null;

        FirebaseRiderRepository.getAllRiders(new FirebaseRiderRepository.ResultCallback<List<FirebaseRiderRepository.RiderAccount>>() {
            @Override
            public void onSuccess(List<FirebaseRiderRepository.RiderAccount> riders) {
                if (riders.isEmpty()) {
                    tvNoRiders.setVisibility(View.VISIBLE);
                    btnConfirmAssign.setEnabled(false);
                    return;
                }

                tvNoRiders.setVisibility(View.GONE);
                btnConfirmAssign.setEnabled(true);

                for (int i = 0; i < riders.size(); i++) {
                    FirebaseRiderRepository.RiderAccount rider = riders.get(i);

                    RadioButton radioButton = new RadioButton(AdmAssignRiderActivity.this);
                    radioButton.setText(rider.name + " (" + rider.email + ")");
                    radioButton.setTextSize(15f);
                    radioButton.setTag(rider.email);
                    radioButton.setPadding(0, 12, 0, 12);

                    radioButton.setOnClickListener(v -> {
                        clearOtherSelections((RadioButton) v);
                        selectedRiderEmail = (String) v.getTag();
                    });

                    if (i == 0) {
                        radioButton.setChecked(true);
                        selectedRiderEmail = rider.email;
                    }

                    riderListContainer.addView(radioButton);
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AdmAssignRiderActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearOtherSelections(RadioButton selected) {
        for (int i = 0; i < riderListContainer.getChildCount(); i++) {
            View child = riderListContainer.getChildAt(i);
            if (child instanceof RadioButton && child != selected) {
                ((RadioButton) child).setChecked(false);
            }
        }
    }

    private void assignOrder() {
        if (selectedRiderEmail == null || selectedRiderEmail.trim().isEmpty()) {
            Toast.makeText(this, "Select a rider first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirmAssign.setEnabled(false);
        FirebaseRiderRepository.assignOrderToRider(
            orderId,
            customerName,
            customerAddress,
                selectedRiderEmail,
                new FirebaseRiderRepository.VoidCallback() {
                    @Override
                    public void onSuccess() {
                        btnConfirmAssign.setEnabled(true);
                        Toast.makeText(AdmAssignRiderActivity.this,
                                "Order assigned successfully",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String message) {
                        btnConfirmAssign.setEnabled(true);
                        Toast.makeText(AdmAssignRiderActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}