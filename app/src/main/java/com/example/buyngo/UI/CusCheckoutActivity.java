package com.example.buyngo.UI;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;

public class CusCheckoutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cus_checkout);

        RadioGroup paymentMethodGroup = findViewById(R.id.paymentMethodGroup);
        View cardPaymentSection = findViewById(R.id.cardPaymentSection);
        EditText cardNumberInput = findViewById(R.id.cardNumberInput);
        EditText cardExpiryInput = findViewById(R.id.cardExpiryInput);
        EditText cardCvvInput = findViewById(R.id.cardCvvInput);

        paymentMethodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioCard) {
                cardPaymentSection.setVisibility(View.VISIBLE);
            } else {
                cardPaymentSection.setVisibility(View.GONE);
                cardNumberInput.setText("");
                cardExpiryInput.setText("");
                cardCvvInput.setText("");
            }
        });

        findViewById(R.id.placeOrderButton).setOnClickListener(v -> {
            int selectedPayment = paymentMethodGroup.getCheckedRadioButtonId();
            if (selectedPayment == -1) {
                Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedPayment == R.id.radioCard) {
                if (TextUtils.isEmpty(cardNumberInput.getText())
                        || TextUtils.isEmpty(cardExpiryInput.getText())
                        || TextUtils.isEmpty(cardCvvInput.getText())) {
                    Toast.makeText(this, "Please enter demo card details", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            String paymentLabel = selectedPayment == R.id.radioCard ? "Credit/Debit Card" : "Cash on Delivery";

            new AlertDialog.Builder(this)
                    .setTitle("Confirm Order")
                    .setMessage("Confirm this order using " + paymentLabel + "?")
                    .setPositiveButton("Confirm", (dialog, which) -> {
                        Intent intent = new Intent(this, CusHomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }
}