package com.example.buyngo.UI;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.buyngo.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class AdmManageRidersActivity extends AppCompatActivity {

    private static final String DB_URL = "https://buyngo-5b43e-default-rtdb.firebaseio.com/";
    private DatabaseReference db;
    private RecyclerView recyclerView;
    private RiderAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private List<RiderItem> riderList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adm_manage_riders);

        db = FirebaseDatabase.getInstance(DB_URL).getReference();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        riderList = new ArrayList<>();

        recyclerView = findViewById(R.id.recyclerRiders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RiderAdapter(riderList);
        recyclerView.setAdapter(adapter);

        loadRiders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRiders();
    }

    private void loadRiders() {
        progressBar.setVisibility(View.VISIBLE);
        db.child("riders").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                riderList.clear();

                if (!snapshot.exists()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    return;
                }

                tvEmpty.setVisibility(View.GONE);
                for (DataSnapshot child : snapshot.getChildren()) {
                    String riderId = child.getKey();
                    String name = child.child("name").getValue(String.class);
                    String email = child.child("email").getValue(String.class);
                    String phone = child.child("phone").getValue(String.class);
                    String vehicle = child.child("vehicle").getValue(String.class);
                    String vehicleNumber = child.child("vehicleNumber").getValue(String.class);
                    String profileImageUrl = child.child("profileImageUrl").getValue(String.class);

                    RiderItem item = new RiderItem(riderId, name, email, phone, vehicle, vehicleNumber, profileImageUrl);
                    riderList.add(item);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdmManageRidersActivity.this,
                        "Failed to load riders: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteRider(RiderItem rider) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Rider")
                .setMessage("Are you sure you want to delete " + rider.name + "?\nThis action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    deleteRiderFromFirebase(rider);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRiderFromFirebase(RiderItem rider) {
        // Delete rider data from Realtime Database
        db.child("riders").child(rider.riderId).removeValue((error, ref) -> {
            if (error != null) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdmManageRidersActivity.this,
                        "Failed to delete rider: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Delete rider profile image from Storage if exists
            if (rider.profileImageUrl != null && !rider.profileImageUrl.isEmpty()) {
                StorageReference imageRef = FirebaseStorage.getInstance()
                        .getReference()
                        .child("rider_profiles")
                        .child(rider.riderId)
                        .child("profile.jpg");

                imageRef.delete().addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AdmManageRidersActivity.this,
                            "Rider deleted successfully",
                            Toast.LENGTH_SHORT).show();
                    loadRiders();
                }).addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AdmManageRidersActivity.this,
                            "Rider deleted but image deletion failed",
                            Toast.LENGTH_SHORT).show();
                    loadRiders();
                });
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdmManageRidersActivity.this,
                        "Rider deleted successfully",
                        Toast.LENGTH_SHORT).show();
                loadRiders();
            }
        });
    }

    private static class RiderItem {
        String riderId;
        String name;
        String email;
        String phone;
        String vehicle;
        String vehicleNumber;
        String profileImageUrl;

        RiderItem(String riderId, String name, String email, String phone,
                  String vehicle, String vehicleNumber, String profileImageUrl) {
            this.riderId = riderId;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.vehicle = vehicle;
            this.vehicleNumber = vehicleNumber;
            this.profileImageUrl = profileImageUrl;
        }
    }

    private class RiderAdapter extends RecyclerView.Adapter<RiderAdapter.RiderViewHolder> {
        private final List<RiderItem> items;

        RiderAdapter(List<RiderItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public RiderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_rider, parent, false);
            return new RiderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RiderViewHolder holder, int position) {
            RiderItem rider = items.get(position);

            holder.tvRiderName.setText(rider.name);
            holder.tvRiderEmail.setText(rider.email);
            holder.tvRiderPhone.setText(rider.phone);
            holder.tvVehicleInfo.setText(rider.vehicle + " - " + rider.vehicleNumber);

            if (rider.profileImageUrl != null && !rider.profileImageUrl.isEmpty()) {
                Glide.with(AdmManageRidersActivity.this)
                        .load(rider.profileImageUrl)
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .into(holder.imgRiderProfile);
            } else {
                holder.imgRiderProfile.setImageResource(R.drawable.ic_launcher_foreground);
            }

            holder.btnDelete.setOnClickListener(v -> deleteRider(rider));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class RiderViewHolder extends RecyclerView.ViewHolder {
            ImageView imgRiderProfile;
            TextView tvRiderName;
            TextView tvRiderEmail;
            TextView tvRiderPhone;
            TextView tvVehicleInfo;
            ImageButton btnDelete;

            RiderViewHolder(@NonNull View itemView) {
                super(itemView);
                imgRiderProfile = itemView.findViewById(R.id.imgRiderProfile);
                tvRiderName = itemView.findViewById(R.id.tvRiderName);
                tvRiderEmail = itemView.findViewById(R.id.tvRiderEmail);
                tvRiderPhone = itemView.findViewById(R.id.tvRiderPhone);
                tvVehicleInfo = itemView.findViewById(R.id.tvVehicleInfo);
                btnDelete = itemView.findViewById(R.id.btnDeleteRider);
            }
        }
    }
}
