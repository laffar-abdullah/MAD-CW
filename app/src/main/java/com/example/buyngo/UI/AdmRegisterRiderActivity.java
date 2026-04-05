package com.example.buyngo.UI;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.buyngo.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AdmRegisterRiderActivity extends AppCompatActivity {

    private static final String TAG = "RIDER_REGISTER";

    private EditText etRiderName;
    private EditText etRiderPhone;
    private EditText etVehicleType;
    private EditText etVehicleNumber;
    private EditText etLicenseExpireDate;
    private EditText etRiderEmail;
    private EditText etRiderPassword;
    private Button btnRegisterRider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adm_register_rider);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etRiderName = findViewById(R.id.etRiderName);
        etRiderPhone = findViewById(R.id.etRiderPhone);
        etVehicleType = findViewById(R.id.etVehicleType);
        etVehicleNumber = findViewById(R.id.etVehicleNumber);
        etLicenseExpireDate = findViewById(R.id.etLicenseExpireDate);
        etRiderEmail = findViewById(R.id.etRiderEmail);
        etRiderPassword = findViewById(R.id.etRiderPassword);
        btnRegisterRider = findViewById(R.id.btnRegisterRider);

        etLicenseExpireDate.setOnClickListener(v -> showDatePicker());
        btnRegisterRider.setOnClickListener(v -> registerRider());
    }

    private void registerRider() {
        String name = etRiderName.getText().toString().trim();
        String phone = etRiderPhone.getText().toString().trim();
        String vehicleType = etVehicleType.getText().toString().trim();
        String vehicleNumber = etVehicleNumber.getText().toString().trim().toUpperCase();
        String licenseExpireDate = etLicenseExpireDate.getText().toString().trim();
        String email = etRiderEmail.getText().toString().trim().toLowerCase();
        String password = etRiderPassword.getText().toString();

        Log.d(TAG, "Attempting rider registration for email=" + email);

        if (TextUtils.isEmpty(name)) {
            etRiderName.setError("Enter rider name");
            etRiderName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            etRiderPhone.setError("Enter phone number");
            etRiderPhone.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(vehicleType)) {
            etVehicleType.setError("Enter vehicle type");
            etVehicleType.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(vehicleNumber)) {
            etVehicleNumber.setError("Enter vehicle number");
            etVehicleNumber.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(licenseExpireDate)) {
            etLicenseExpireDate.setError("Select license expiry date");
            etLicenseExpireDate.requestFocus();
            return;
        }
        if (isLicenseExpired(licenseExpireDate)) {
            etLicenseExpireDate.setError("License has already expired - must be future date");
            etLicenseExpireDate.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRiderEmail.setError("Enter a valid email");
            etRiderEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etRiderPassword.setError("Password must be at least 6 characters");
            etRiderPassword.requestFocus();
            return;
        }

        btnRegisterRider.setEnabled(false);
        FirebaseRiderRepository.registerRider(
                name,
                phone,
                vehicleType,
                vehicleNumber,
                licenseExpireDate,
                email,
                password,
                new FirebaseRiderRepository.ResultCallback<FirebaseRiderRepository.RiderAccount>() {
                    @Override
                    public void onSuccess(FirebaseRiderRepository.RiderAccount result) {
                        btnRegisterRider.setEnabled(true);
                        Log.i(TAG, "Saved to Firebase successfully. riderId=" + result.riderId);
                        Toast.makeText(AdmRegisterRiderActivity.this,
                                "Rider registered successfully",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String message) {
                        btnRegisterRider.setEnabled(true);
                        Log.e(TAG, "Firebase save failed: " + message);
                        Toast.makeText(AdmRegisterRiderActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Show calendar to select license expiry date
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            etLicenseExpireDate.setText(dateFormat.format(selectedDate.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    // Check if license date is expired compared to today
    private boolean isLicenseExpired(String dateString) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date licenseDate = dateFormat.parse(dateString);
            Date today = new Date();
            return licenseDate.before(today);

        } catch (Exception e) {

            Log.e(TAG, "Error parsing date", e);

            return false;
        }
    }
}

