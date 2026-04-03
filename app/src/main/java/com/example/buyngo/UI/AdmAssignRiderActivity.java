package com.example.buyngo.UI;

import android.os.Bundle;
<<<<<<< HEAD
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
=======
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
>>>>>>> cc33148f16efd7cd1a6422a65c9b53b87be2e710
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.buyngo.Model.Order;
import com.example.buyngo.Model.Rider;
import com.example.buyngo.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import java.util.List;

public class AdmAssignRiderActivity extends AppCompatActivity {
    private String orderId;
    private Order currentOrder;
    private List<Rider> riderList = new ArrayList<>();
    private LinearLayout riderListContainer;
    private RadioGroup riderRadioGroup;
    private String selectedRiderId;
    private FirebaseDatabase firebaseDatabase;

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

        firebaseDatabase = FirebaseDatabase.getInstance();
        orderId = getIntent().getStringExtra("orderId");
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        riderListContainer = findViewById(R.id.riderListContainer);
<<<<<<< HEAD
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
=======
        Button btnConfirmAssign = findViewById(R.id.btnConfirmAssign);

        loadOrderDetails();
        loadAvailableRiders();

        btnConfirmAssign.setOnClickListener(v -> confirmAssignRider());
>>>>>>> cc33148f16efd7cd1a6422a65c9b53b87be2e710
    }

    private void loadOrderDetails() {
        firebaseDatabase.getReference("orders").child(orderId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        currentOrder = snapshot.getValue(Order.class);
                        if (currentOrder != null) {
                            displayOrderDetails();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(AdmAssignRiderActivity.this, "Error loading order", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayOrderDetails() {
        TextView tvOrderId = findViewById(R.id.tvOrderId);
        TextView tvOrderCustomer = findViewById(R.id.tvOrderCustomer);
        
        tvOrderId.setText("Order #" + currentOrder.getOrderId());
        tvOrderCustomer.setText("Customer: " + currentOrder.getCustomerName());
    }

    private void loadAvailableRiders() {
        firebaseDatabase.getReference("riders")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        riderList.clear();
                        
                        if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                            findViewById(R.id.tvNoRiders).setVisibility(android.view.View.VISIBLE);
                            riderListContainer.setVisibility(android.view.View.GONE);
                            return;
                        }

                        for (DataSnapshot riderSnapshot : snapshot.getChildren()) {
                            Rider rider = riderSnapshot.getValue(Rider.class);
                            if (rider != null) {
                                riderList.add(rider);
                            }
                        }
                        
                        displayRiderList();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(AdmAssignRiderActivity.this, "Error loading riders", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayRiderList() {
        riderListContainer.removeAllViews();
        riderRadioGroup = new RadioGroup(this);
        riderRadioGroup.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        for (Rider rider : riderList) {
            RadioButton rb = new RadioButton(this);
            rb.setTag(rider.getRiderId());
            rb.setText(rider.getRiderName() + " - " + rider.getPhoneNumber());
            rb.setTextSize(14);
            rb.setPadding(16, 12, 16, 12);
            rb.setTextColor(getResources().getColor(R.color.text_dark));
            
            riderRadioGroup.addView(rb);
        }

        riderRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selected = findViewById(checkedId);
            if (selected != null) {
                selectedRiderId = (String) selected.getTag();
            }
        });

        riderListContainer.addView(riderRadioGroup);
    }

    private void confirmAssignRider() {
        if (selectedRiderId == null || selectedRiderId.isEmpty()) {
            Toast.makeText(this, "Please select a rider", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find selected rider details
        Rider selectedRider = riderList.stream()
                .filter(r -> r.getRiderId().equals(selectedRiderId))
                .findFirst()
                .orElse(null);

        if (selectedRider == null) {
            Toast.makeText(this, "Error: Rider not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update order with rider assignment
        currentOrder.setRiderId(selectedRiderId);
        currentOrder.setRiderName(selectedRider.getRiderName());
        currentOrder.setStatus("Assigned");
        currentOrder.setUpdatedAt(System.currentTimeMillis());

        // Save to Firebase
        firebaseDatabase.getReference("orders").child(orderId).setValue(currentOrder)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AdmAssignRiderActivity.this, 
                            "Rider assigned successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdmAssignRiderActivity.this, 
                            "Failed to assign rider", Toast.LENGTH_SHORT).show();
                });
    }
}
