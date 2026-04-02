package com.example.buyngo.UI;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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

public class AdmAssignRiderActivity extends AppCompatActivity {
    private String orderId;
    private Order currentOrder;
    private List<Rider> riderList = new ArrayList<>();
    private LinearLayout riderListContainer;
    private RadioGroup riderRadioGroup;
    private String selectedRiderId;
    private FirebaseDatabase firebaseDatabase;

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
        Button btnConfirmAssign = findViewById(R.id.btnConfirmAssign);

        loadOrderDetails();
        loadAvailableRiders();

        btnConfirmAssign.setOnClickListener(v -> confirmAssignRider());
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
