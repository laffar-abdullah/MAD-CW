package com.example.buyngo.Model;

// Stores customer reviews and ratings for orders
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
