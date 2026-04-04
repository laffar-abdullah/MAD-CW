# Project Flow - Simple Diagram

## What is this project?
BUY'N'GO - A grocery delivery app

## Main Features
1. **Sign Up** - Create account
2. **Shop** - Browse and add items to cart
3. **Checkout** - Place order with payment
4. **Track** - See delivery status in real-time
5. **Feedback** - Rate delivery (MAIN FEATURE)

---

## Simple Flow

```
CUSTOMER JOURNEY
================

1. SIGNUP
   Customer → Fill form → Create Firebase account → Save profile

2. SHOPPING  
   Home screen → See products from Firebase → Add to cart (local)

3. CHECKOUT
   Cart → Add address → Choose payment → Place order → Save to Firebase

4. TRACKING
   My Orders → See status updates in real-time (Firebase listener)

5. FEEDBACK (MAIN FEATURE)
   ├─ Automatic: After delivery → Rate 1-5 stars → Submit
   └─ Anonymous: Menu → Rate 1-5 stars → Submit (no order details)
```

---

## Database Structure (Simple)

```
/users/{customerId}/
  - email
  - name
  - phone
  - address

/products/{productId}/
  - name
  - price
  - category

/orders/{orderId}/
  - customerId
  - items
  - status
  - rider

/feedbacks/{feedbackId}/
  - rating
  - comment
  - anonymous (true/false)
```

---

## Key Points

- **Cart**: Stored locally on phone (NOT Firebase)
- **Orders**: Saved to Firebase when customer checks out
- **Feedback**: Can be order-based OR anonymous
- **Real-time Updates**: Rider updates status → Customer sees instantly

---

## Main Feature: Feedback System

Why is feedback the main feature?

1. **Two Modes**: Automatic (after delivery) + Anonymous (anytime)
2. **Real-time**: Saved and visible to admin
3. **Quality Control**: Tracks delivery performance
4. **Customer-friendly**: Easy 1-5 star rating
