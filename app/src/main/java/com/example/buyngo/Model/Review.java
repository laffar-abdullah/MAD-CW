ckage com.example.buyngo.Model;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 *                            REVIEW MODEL FILE
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * WHAT THIS FILE DOES:
 * This file defines the structure of a customer review/rating. When customer
 * completes delivery and rates their experience, this Review model stores that data.
 * 
 * HOW IT CONNECTS TO FIREBASE:
 * 1. Order status changes to "Delivered" (rider confirmed delivery)
 * 2. CusFeedbackActivity shows rating screen to customer
 * 3. Customer gives 1-5 star rating and optional comment
 * 4. App creates Review object with rating, comment, order details
 * 5. Saves to TWO places in Firebase:
 *    - /reviews/{reviewId} (for rider to see feedback about them)
 *    - /feedbacks/{feedbackId} (for admin to view all feedback)
 * 6. Order status updated to "Delivered Successfully" (customer confirmed)
 * 
 * HOW CUSTOMER MODULE USES IT:
 * - CusFeedbackActivity: Creates Review from star rating and comment
 * - FirebaseRiderRepository: Saves review to rider's collection
 * - Admin Dashboard: Views all reviews in /feedbacks/ to monitor quality
 * - Rider App: Sees reviews customers gave about their deliveries
 * 
 * FEEDBACK FLOW:
 * Order Delivered → CusFeedbackActivity shown → Customer rates & comments
 *                             ↓
 *                       Review object created
 *                             ↓
 *       Saved to /reviews/ AND /feedbacks/ in Firebase
 *                             ↓
 *          Admin can see feedback, Rider can see review about them
 * 
 * KEY FIELDS:
 * - reviewId: Unique identifier for this review
 * - orderId: Which order this review is for
 * - userId: Which customer gave this review
 * - userEmail: Customer email
 * - rating: Star rating (1.0 to 5.0)
 * - comment: Customer's written feedback
 * - timestamp: When review was submitted
 * 
 * SPECIAL HANDLING FOR ANONYMOUS REVIEWS:
 * When customer gives feedback from nav_reviews tab (without an order),
 * reviewId = "anonymous" and userId = "anonymous" are used.
 * ═══════════════════════════════════════════════════════════════════════════════
 */
public class Review {
    private String reviewId;
    private String orderId;
    private String userId;
    private String userEmail;
    private float rating;
    private String comment;
    private long timestamp;

    public Review() {
    }

    public Review(String orderId, String userId, String userEmail, float rating, String comment) {
        this.orderId = orderId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = System.currentTimeMillis();
    }

    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getRatingStars() {
        int stars = (int) rating;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stars; i++) {
            sb.append("★");
        }
        return sb.toString();
    }

    public String getRatingLabel() {
        if (rating == 1) return "Poor";
        else if (rating == 2) return "Fair";
        else if (rating == 3) return "Good";
        else if (rating == 4) return "Very Good";
        else if (rating == 5) return "Excellent";
        return "Not rated";
    }
}
