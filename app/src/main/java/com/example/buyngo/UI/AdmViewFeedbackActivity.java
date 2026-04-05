ackage com.example.buyngo.UI;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.buyngo.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdmViewFeedbackActivity extends AppCompatActivity {

    private DatabaseReference db;
    private RecyclerView recyclerView;
    private FeedbackAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvAverageRating, tvTotalFeedbacks;
    private TextView tvPositiveCount, tvNeutralCount, tvNegativeCount;
    private TextView tvAnalysisSummary;
    private TextView chipAll, chipPositive, chipNeutral, chipNegative, chipFlagged;

    private final List<DataSnapshot> allFeedbacks      = new ArrayList<>();
    private final List<DataSnapshot> filteredFeedbacks = new ArrayList<>();
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adm_view_feedback);

        db = FirebaseDatabase.getInstance("https://buyngo-5b43e-default-rtdb.firebaseio.com/").getReference();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar      = findViewById(R.id.progressBar);
        tvEmpty          = findViewById(R.id.tvEmpty);
        tvAverageRating  = findViewById(R.id.tvAverageRating);
        tvTotalFeedbacks = findViewById(R.id.tvTotalFeedbacks);
        tvPositiveCount  = findViewById(R.id.tvPositiveCount);
        tvNeutralCount   = findViewById(R.id.tvNeutralCount);
        tvNegativeCount  = findViewById(R.id.tvNegativeCount);
        tvAnalysisSummary = findViewById(R.id.tvAnalysisSummary);

        // Filter buttons (plain TextViews)
        chipAll      = findViewById(R.id.chipAll);
        chipPositive = findViewById(R.id.chipPositive);
        chipNeutral  = findViewById(R.id.chipNeutral);
        chipNegative = findViewById(R.id.chipNegative);
        chipFlagged  = findViewById(R.id.chipFlagged);

        chipAll.setOnClickListener(v -> applyFilter("all"));
        chipPositive.setOnClickListener(v -> applyFilter("positive"));
        chipNeutral.setOnClickListener(v -> applyFilter("neutral"));
        chipNegative.setOnClickListener(v -> applyFilter("negative"));
        chipFlagged.setOnClickListener(v -> applyFilter("flagged"));

        recyclerView = findViewById(R.id.recyclerFeedbacks);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FeedbackAdapter(filteredFeedbacks);
        recyclerView.setAdapter(adapter);

        // Regular FAB for analysis report
        FloatingActionButton fabAnalyze = findViewById(R.id.fabAnalyze);
        fabAnalyze.setOnClickListener(v -> showAnalysisReport());

        loadFeedbacks();
    }

    private void loadFeedbacks() {
        progressBar.setVisibility(View.VISIBLE);
        db.child("feedbacks").orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        progressBar.setVisibility(View.GONE);
                        allFeedbacks.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            allFeedbacks.add(0, child); // newest first
                        }
                        updateStats();
                        applyFilter(currentFilter);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AdmViewFeedbackActivity.this, "Failed to load feedbacks", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateStats() {
        float totalRating = 0;
        int count = 0, positive = 0, neutral = 0, negative = 0;

        for (DataSnapshot snap : allFeedbacks) {
            Object ratingObj = snap.child("rating").getValue();
            if (ratingObj != null) {
                float r = Float.parseFloat(ratingObj.toString());
                totalRating += r;
                count++;
                if (r >= 4) positive++;
                else if (r == 3) neutral++;
                else negative++;
            }
        }

        if (count > 0) {
            float avg = totalRating / count;
            tvAverageRating.setText(String.format(Locale.getDefault(), "%.1f ★", avg));
            tvTotalFeedbacks.setText(count + " review" + (count > 1 ? "s" : ""));
            tvPositiveCount.setText(String.valueOf(positive));
            tvNeutralCount.setText(String.valueOf(neutral));
            tvNegativeCount.setText(String.valueOf(negative));

            float positiveRate = (positive * 100f) / count;
            String trend;
            if (positiveRate >= 70) trend = "Customers are largely satisfied. Keep up the good work!";
            else if (positiveRate >= 40) trend = "Mixed feedback. Review neutral and negative comments for improvement areas.";
            else trend = "High dissatisfaction detected. Urgent attention needed on complaints.";
            tvAnalysisSummary.setText(trend);
            tvEmpty.setVisibility(View.GONE);
        } else {
            tvAverageRating.setText("—");
            tvTotalFeedbacks.setText("No reviews yet");
            tvPositiveCount.setText("0");
            tvNeutralCount.setText("0");
            tvNegativeCount.setText("0");
            tvAnalysisSummary.setText("No feedback data available yet.");
            tvEmpty.setVisibility(View.VISIBLE);
        }
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        filteredFeedbacks.clear();

        for (DataSnapshot snap : allFeedbacks) {
            Object ratingObj = snap.child("rating").getValue();
            Boolean flagged  = snap.child("flagged").getValue(Boolean.class);
            float rating = ratingObj != null ? Float.parseFloat(ratingObj.toString()) : 0;

            switch (filter) {
                case "positive": if (rating >= 4) filteredFeedbacks.add(snap); break;
                case "neutral":  if (rating == 3) filteredFeedbacks.add(snap); break;
                case "negative": if (rating <= 2) filteredFeedbacks.add(snap); break;
                case "flagged":  if (Boolean.TRUE.equals(flagged)) filteredFeedbacks.add(snap); break;
                default:         filteredFeedbacks.add(snap); break;
            }
        }

        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(filteredFeedbacks.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showAnalysisReport() {
        if (allFeedbacks.isEmpty()) {
            Toast.makeText(this, "No feedback data to analyze", Toast.LENGTH_SHORT).show();
            return;
        }

        int count = allFeedbacks.size();
        int positive = 0, neutral = 0, negative = 0, flagged = 0;
        float total = 0;

        for (DataSnapshot snap : allFeedbacks) {
            Object r = snap.child("rating").getValue();
            Boolean f = snap.child("flagged").getValue(Boolean.class);
            if (r != null) {
                float rating = Float.parseFloat(r.toString());
                total += rating;
                if (rating >= 4) positive++;
                else if (rating == 3) neutral++;
                else negative++;
            }
            if (Boolean.TRUE.equals(f)) flagged++;
        }

        float avg = total / count;
        String report =
                "FEEDBACK ANALYSIS REPORT\n\n" +
                        "Total Reviews      : " + count + "\n" +
                        "Average Rating     : " + String.format(Locale.getDefault(), "%.1f / 5.0", avg) + "\n\n" +
                        "Positive (4-5 star): " + positive + " (" + (positive * 100 / count) + "%)\n" +
                        "Neutral  (3 star)  : " + neutral  + " (" + (neutral  * 100 / count) + "%)\n" +
                        "Negative (1-2 star): " + negative + " (" + (negative * 100 / count) + "%)\n" +
                        "Flagged            : " + flagged  + "\n\n" +
                        "RECOMMENDATION\n" +
                        getRecommendation(avg, negative, count);

        new AlertDialog.Builder(this)
                .setTitle("Analysis Report")
                .setMessage(report)
                .setPositiveButton("Close", null)
                .show();
    }

    private String getRecommendation(float avg, int negative, int total) {
        if (avg >= 4.5f) return "Excellent performance! Maintain current standards.";
        if (avg >= 3.5f) return "Good overall. Address neutral feedback to push rating higher.";
        if (avg >= 2.5f) return "Needs improvement. Focus on resolving the top complaints.";
        return "Critical: " + negative + " out of " + total + " customers are dissatisfied. Immediate action required.";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Adapter
    // ─────────────────────────────────────────────────────────────────────────

    private class FeedbackAdapter extends RecyclerView.Adapter<FeedbackAdapter.FeedbackVH> {

        private final List<DataSnapshot> items;

        FeedbackAdapter(List<DataSnapshot> items) { this.items = items; }

        @NonNull
        @Override
        public FeedbackVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_feedback, parent, false);
            return new FeedbackVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull FeedbackVH h, int pos) {
            DataSnapshot snap = items.get(pos);

            String email      = snap.child("userEmail").getValue(String.class);
            String comment    = snap.child("comment").getValue(String.class);
            String adminReply = snap.child("adminReply").getValue(String.class);
            Object ratingObj  = snap.child("rating").getValue();
            Object tsObj      = snap.child("timestamp").getValue();
            Boolean flagged   = snap.child("flagged").getValue(Boolean.class);

            h.tvEmail.setText(email != null ? email : "Anonymous");
            h.tvComment.setText((comment != null && !comment.isEmpty()) ? comment : "No comment left.");
            h.ratingBar.setRating(ratingObj != null ? Float.parseFloat(ratingObj.toString()) : 0);

            if (tsObj != null) {
                String date = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        .format(new Date(Long.parseLong(tsObj.toString())));
                h.tvDate.setText(date);
            }

            float rating = ratingObj != null ? Float.parseFloat(ratingObj.toString()) : 0;
            if (rating >= 4) {
                h.tvRatingBadge.setText("Positive");
                h.tvRatingBadge.setBackgroundColor(0xFF4CAF50);
            } else if (rating == 3) {
                h.tvRatingBadge.setText("Neutral");
                h.tvRatingBadge.setBackgroundColor(0xFFFF9800);
            } else {
                h.tvRatingBadge.setText("Negative");
                h.tvRatingBadge.setBackgroundColor(0xFFF44336);
            }

            h.tvFlagIndicator.setVisibility(Boolean.TRUE.equals(flagged) ? View.VISIBLE : View.GONE);

            if (adminReply != null && !adminReply.isEmpty()) {
                h.layoutAdminReply.setVisibility(View.VISIBLE);
                h.tvAdminReply.setText("Admin: " + adminReply);
            } else {
                h.layoutAdminReply.setVisibility(View.GONE);
            }

            h.btnReply.setOnClickListener(v -> showReplyDialog(snap));
            h.btnFlag.setOnClickListener(v -> toggleFlag(snap, Boolean.TRUE.equals(flagged)));
            h.btnDelete.setOnClickListener(v -> confirmDelete(snap));
        }

        @Override
        public int getItemCount() { return items.size(); }

        class FeedbackVH extends RecyclerView.ViewHolder {
            TextView tvEmail, tvComment, tvDate, tvRatingBadge, tvFlagIndicator, tvAdminReply;
            TextView btnReply, btnFlag, btnDelete;
            RatingBar ratingBar;
            LinearLayout layoutAdminReply;

            FeedbackVH(@NonNull View itemView) {
                super(itemView);
                tvEmail          = itemView.findViewById(R.id.tvUserEmail);
                tvComment        = itemView.findViewById(R.id.tvComment);
                tvDate           = itemView.findViewById(R.id.tvDate);
                tvRatingBadge    = itemView.findViewById(R.id.tvRatingBadge);
                tvFlagIndicator  = itemView.findViewById(R.id.tvFlagIndicator);
                tvAdminReply     = itemView.findViewById(R.id.tvAdminReply);
                btnReply         = itemView.findViewById(R.id.btnReply);
                btnFlag          = itemView.findViewById(R.id.btnFlag);
                btnDelete        = itemView.findViewById(R.id.btnDelete);
                ratingBar        = itemView.findViewById(R.id.ratingBar);
                layoutAdminReply = itemView.findViewById(R.id.layoutAdminReply);
            }
        }
    }

    private void showReplyDialog(DataSnapshot snap) {
        String existing = snap.child("adminReply").getValue(String.class);
        EditText input = new EditText(this);
        input.setHint("Type your reply...");
        input.setPadding(40, 20, 40, 20);
        if (existing != null) input.setText(existing);

        new AlertDialog.Builder(this)
                .setTitle("Reply to Customer")
                .setView(input)
                .setPositiveButton("Send Reply", (dialog, which) -> {
                    String reply = input.getText().toString().trim();
                    if (reply.isEmpty()) {
                        Toast.makeText(this, "Reply cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.child("feedbacks").child(snap.getKey()).child("adminReply").setValue(reply)
                            .addOnSuccessListener(u -> Toast.makeText(this, "Reply saved!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to save reply", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleFlag(DataSnapshot snap, boolean currentlyFlagged) {
        boolean newFlag = !currentlyFlagged;
        db.child("feedbacks").child(snap.getKey()).child("flagged").setValue(newFlag)
                .addOnSuccessListener(u -> Toast.makeText(this,
                        newFlag ? "Feedback flagged!" : "Flag removed", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update flag", Toast.LENGTH_SHORT).show());
    }

    private void confirmDelete(DataSnapshot snap) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Feedback")
                .setMessage("Are you sure you want to delete this feedback?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.child("feedbacks").child(snap.getKey()).removeValue()
                            .addOnSuccessListener(u -> Toast.makeText(this, "Feedback deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
