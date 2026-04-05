package com.example.buyngo.UI;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.buyngo.R;

import java.util.Calendar;

public class AdmRegisterRiderActivity extends AppCompatActivity {

    private static final String TAG = "RIDER_REGISTER";

    private EditText etRiderName;
    private EditText etRiderPhone;
    private EditText etVehicleType;
    private EditText etVehicleNumber;
    private EditText etRiderBirthdate;
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
        etRiderBirthdate = findViewById(R.id.etRiderBirthdate);
        etRiderEmail = findViewById(R.id.etRiderEmail);
        etRiderPassword = findViewById(R.id.etRiderPassword);
        btnRegisterRider = findViewById(R.id.btnRegisterRider);

        // Add automatic date formatting for birthdate
        etRiderBirthdate.addTextChangedListener(birthdateWatcher);

        btnRegisterRider.setOnClickListener(v -> registerRider());
    }

    private void registerRider() {
        String name = etRiderName.getText().toString().trim();
        String phone = etRiderPhone.getText().toString().trim();
        String vehicleType = etVehicleType.getText().toString().trim();
        String vehicleNumber = etVehicleNumber.getText().toString().trim().toUpperCase();
        String birthdate = etRiderBirthdate.getText().toString().trim();
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
        if (TextUtils.isEmpty(birthdate)) {
            etRiderBirthdate.setError("Enter birthdate");
            etRiderBirthdate.requestFocus();
            return;
        }
        if (!isBirthdateValid(birthdate)) {
            etRiderBirthdate.setError("Enter valid date (YYYY-MM-DD)");
            etRiderBirthdate.requestFocus();
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
                birthdate,
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

    private boolean isBirthdateValid(String birthdate) {
        if (!birthdate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return false;
        }
        try {
            String[] parts = birthdate.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);

            if (month < 1 || month > 12 || day < 1 || day > 31) {
                return false;
            }
            // if 18 years old
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            if (year > currentYear - 18) {
                return false; //if not young
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void formatBirthdateInput(Editable s) {
        // Remove all non-digit characters
        String input = s.toString().replaceAll("[^0-9]", "");
        
        // Limit to 8 digits only (YYYYMMDD)
        if (input.length() > 8) {
            input = input.substring(0, 8);
        }

        StringBuilder formatted = new StringBuilder();
        
        // Auto-format as YYYY-MM-DD
        if (input.length() <= 4) {
            // Just year
            formatted.append(input);
        } else if (input.length() <= 6) {
            // Year and month
            formatted.append(input.substring(0, 4)).append("-");
            formatted.append(input.substring(4));
        } else {
            // Full date with day
            formatted.append(input.substring(0, 4)).append("-");
            formatted.append(input.substring(4, 6)).append("-");
            formatted.append(input.substring(6));
        }

        // Update without triggering listener recursion
        if (!s.toString().equals(formatted.toString())) {
            etRiderBirthdate.removeTextChangedListener(birthdateWatcher);
            etRiderBirthdate.setText(formatted.toString());
            etRiderBirthdate.setSelection(formatted.toString().length());
            etRiderBirthdate.addTextChangedListener(birthdateWatcher);
        }
    }

    private TextWatcher birthdateWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            formatBirthdateInput(s);
        }
    };
}