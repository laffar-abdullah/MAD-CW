package com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.buyngo.R;

import java.text.DateFormat;
import java.util.List;

public class RidDeliveryHistoryActivity extends AppCompatActivity {
    private LinearLayout historyContainer;
    private TextView txtEmptyHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Restrict history screen to logged-in riders.
        if (!RiderSessionStore.isLoggedIn(this)) {
            startActivity(new Intent(this, RidLoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.rid_delivery_history);

        historyContainer = findViewById(R.id.historyContainer);
        txtEmptyHistory = findViewById(R.id.txtEmptyHistory);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Bottom navigation shortcuts jump back to the other rider screens.
        findViewById(R.id.navDashboard).setOnClickListener(v ->
            startActivity(new Intent(this, RidDashboardActivity.class)));
        findViewById(R.id.navReviews).setOnClickListener(v ->
            startActivity(new Intent(this, RidReviewsActivity.class)));
        findViewById(R.id.navProfile).setOnClickListener(v ->
            startActivity(new Intent(this, RidProfileActivity.class)));

        renderHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderHistory();
    }

    private void renderHistory() {
        // Rebuild list from the rider-scoped delivery store each time.
        historyContainer.removeAllViews();
        List<OrderStatusStore.DeliveryRecord> records = OrderStatusStore.getDeliveryHistory(this);

        if (records.isEmpty()) {
            // Empty state stays visible when this rider has not completed any deliveries.
            txtEmptyHistory.setVisibility(View.VISIBLE);
            return;
        }

        txtEmptyHistory.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(this);

        for (int i = records.size() - 1; i >= 0; i--) {
            // Newest deliveries are rendered first.
            OrderStatusStore.DeliveryRecord record = records.get(i);
            View card = inflater.inflate(R.layout.item_delivery_history, historyContainer, false);

            TextView txtOrderId = card.findViewById(R.id.txtHistoryOrderId);
            TextView txtCustomer = card.findViewById(R.id.txtHistoryCustomer);
            TextView txtAddress = card.findViewById(R.id.txtHistoryAddress);
            TextView txtDate = card.findViewById(R.id.txtHistoryDate);

            txtOrderId.setText("Order #" + record.orderId);
            txtCustomer.setText("Customer: " + record.customerName);
            txtAddress.setText("Address: " + record.customerAddress);
            txtDate.setText("Date: " + dateFormat.format(record.deliveredAt));

            historyContainer.addView(card);
        }
    }
}
