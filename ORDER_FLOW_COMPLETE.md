# BuyNgo Complete Order Management Flow

## Overview
This document describes the complete order lifecycle from customer placement through delivery completion and review.

---

## 1. CUSTOMER PLACES ORDER
**Screen**: CusHomeActivity (Home tab)
**Action**: Customer adds items to cart and places order

**What happens**:
- Order created with status: **"Pending"**
- Order saved to Firebase with:
  - Order ID (timestamp-based)
  - Customer name and address
  - Items and total amount
  - Initial status: "Pending"

**Order data structure**:
```
/orders/{orderId}
├── customerId: "laffar"
├── customerName: "Laffar"
├── customerAddress: "123 Main St"
├── items: [{name: "soap", price: 120}]
├── totalAmount: 120
└── status: "Pending"
```

---

## 2. ADMIN SEES PENDING ORDER
**Screen**: AdmOrderManagementActivity (Admin panel)
**Real-time update**: Using `addValueEventListener`

**What admin sees**:
- List of all pending orders
- Order card shows:
  - Order ID
  - Customer name and address
  - Items and total price
  - Status: "Pending"
  - **"Confirm Order"** button (green, enabled)
  - **"Assign Rider"** button (visible but requires confirmation first)

---

## 3. ADMIN CONFIRMS ORDER
**Screen**: AdmOrderManagementActivity
**Action**: Admin clicks "Confirm Order" button

**What happens**:
- Order status updated to: **"Confirmed"**
- **"Confirm Order" button automatically HIDES** (can't re-confirm)
- Button label changes to show confirmed
- Refresh in real-time using listener

**Order data update**:
```
/orders/{orderId}
└── status: "Confirmed" ← CHANGED
```

---

## 4. ADMIN ASSIGNS RIDER
**Screen**: AdmOrderManagementActivity → AdmAssignRiderActivity
**Action**: Admin clicks "Assign Rider" button

**What happens**:
- Opens rider selection dialog
- Admin selects a rider (e.g., "rider@email.com")
- Order is linked to rider with:
  - `assignedRiderEmail`: "rider@email.com"
  - `riderId`: "Rider Name"
  - `riderName`: "Rider Name"

**Order data update**:
```
/orders/{orderId}
├── status: "Confirmed" ← UNCHANGED
├── assignedRiderEmail: "rider@email.com" ← NEW
├── riderId: "Rider Name" ← NEW
└── riderName: "Rider Name" ← NEW
```

---

## 5. RIDER SEES ASSIGNED ORDER IN DASHBOARD
**Screen**: RidDashboardActivity (Rider's home)
**Real-time update**: Fetches orders where `assignedRiderEmail` matches rider's email

**What rider sees**:
- **"Assigned Tasks"** section
- Order card shows:
  - Order ID: "ORD-123456"
  - Customer: "Laffar"
  - Address: "123 Main St"
  - Status: "Awaiting Pickup" (default for confirmed orders)
  - Can click to view details

---

## 6. CUSTOMER SEES CONFIRMED ORDER WITH RIDER DETAILS
**Screen**: CusTrackingActivity (Orders tab)
**Real-time update**: Using `addValueEventListener`

**What customer sees**:
- Order card shows:
  - Order ID: "ORD-123456"
  - **Status: "Confirmed"** ← Now shows confirmed, not pending
  - Items and total
  - **Assigned Rider section**:
    - Rider Name: "Ali Ahmed"
  - **"I have received the order"** button (green, enabled)

**Why this works**:
- Customer sees rider details when status is "Confirmed" or "Delivered"
- Real-time listener updates immediately when admin assigns rider

---

## 7. RIDER UPDATES DELIVERY STATUS
**Screen**: RidStatusUpdateActivity
**Flow**: Awaiting Pickup → Picked Up → On the Way → Delivered

**Step 1**: Rider clicks "Mark as Picked Up"
- Status updates to: **"Picked Up"**
- Next button enabled

**Step 2**: Rider clicks "Mark as On the Way"  
- Status updates to: **"On the Way"**
- Next button enabled

**Step 3**: Rider clicks "Mark as Delivered"
- Status updates to: **"Delivered"**
- Screen closes, rider returns to dashboard
- Order no longer shows in active tasks

**Order data throughout**:
```
/orders/{orderId}
└── status: "Awaiting Pickup" → "Picked Up" → "On the Way" → "Delivered"
```

---

## 8. CUSTOMER SEES DELIVERY PROGRESS & RECEIVES ORDER
**Screen**: CusTrackingActivity (real-time updates)

**Customer sees status progression**:
- Status changes to: "Picked Up" (in real-time)
- Status changes to: "On the Way"
- Status changes to: "Delivered"

**When status is "Delivered"**:
- **"I have received the order"** button is available
- Customer clicks button

**What happens when customer clicks**:
- Order status changed to: **"Received"**
- Toast: "Order received! Please add a review."
- **Automatically navigates to CusFeedbackActivity**
- Review screen opens with order ID pre-filled
- Review is **MANDATORY** (can't skip without confirmation)

**Order data update**:
```
/orders/{orderId}
└── status: "Delivered" → "Received" ← Customer marks received
```

---

## 9. CUSTOMER ADDS REVIEW (MANDATORY)
**Screen**: CusFeedbackActivity
**Title**: "Rate Your Order"

**Customer sees**:
- 5-star rating control (must select 1-5 stars)
- Optional text field for comment
- **"Submit Review"** button (green, enabled only when rating selected)
- **"Skip for now"** link (shows confirmation dialog for mandatory reviews)

**Customer interaction**:
1. Taps 1-5 stars (e.g., 5 stars)
2. Optionally writes comment (e.g., "Great delivery!")
3. Clicks **"Submit Review"**

**What happens on submit**:
- Review saved to database:
  ```
  /reviews/{reviewId}
  ├── orderId: "ORD-123456"
  ├── riderEmail: "rider@email.com"
  ├── rating: 5
  ├── comment: "Great delivery!"
  └── createdAt: timestamp
  ```
- Order status changed to: **"Delivered Successfully"**
- Toast: "Thanks for your feedback!"
- Screen closes, returns to home

**Order final data**:
```
/orders/{orderId}
└── status: "Delivered Successfully" ← FINAL STATUS
```

---

## 10. ORDER MARKED AS COMPLETED
**Screen**: CusTrackingActivity (Orders tab)

**Customer sees**:
- Order card shows:
  - Order ID
  - Items and total
  - **Status: "Delivered Successfully"** (with green checkmark ✓)
  - **No buttons** (can't take any action)
  - Order is complete and closed

---

## Complete Status Lifecycle

```
PENDING (Admin sees)
    ↓ [Admin clicks "Confirm Order"]
CONFIRMED (Customer sees with rider assignment pending)
    ↓ [Admin assigns rider]
CONFIRMED + Rider Assigned (Customer sees rider details)
    ↓ [Rider updates status]
AWAITING PICKUP
    ↓ [Rider clicked "Picked Up"]
PICKED UP
    ↓ [Rider clicked "On the Way"]
ON THE WAY (Customer sees delivery progress)
    ↓ [Rider clicked "Delivered"]
DELIVERED (Customer sees "I have received" button)
    ↓ [Customer clicks "I have received"]
RECEIVED (Customer must add review)
    ↓ [Customer submits review]
DELIVERED SUCCESSFULLY (Order closed)
```

---

## Key Features Implemented

✅ **Real-time order updates** - All screens use Firebase listeners
✅ **Admin order confirmation** - One-click confirmation, button hides after
✅ **Rider assignment** - Orders linked to specific rider via email
✅ **Rider dashboard** - Shows only assigned orders (client-side filtering, no index needed)
✅ **Customer tracking** - Real-time status updates from rider
✅ **Mandatory review** - Can't skip review after order received
✅ **Order closure** - Automatically marked "Delivered Successfully" after review
✅ **Complete lifecycle** - From Pending through final Delivered Successfully

---

## Firebase Data Structure

```
buyngo-5b43e-default-rtdb
├── orders/
│   └── {orderId}
│       ├── customerId
│       ├── customerName
│       ├── customerAddress
│       ├── items[]
│       ├── totalAmount
│       ├── status (Pending → Confirmed → ... → Delivered Successfully)
│       ├── assignedRiderEmail (after assignment)
│       ├── riderId (after assignment)
│       └── riderName (after assignment)
│
├── riders/
│   └── {riderId}
│       ├── email
│       ├── name
│       └── phone
│
└── reviews/
    └── {reviewId}
        ├── orderId
        ├── riderEmail
        ├── rating
        ├── comment
        └── createdAt
```

---

## Testing Checklist

- [ ] Customer places order → Status "Pending"
- [ ] Admin sees pending order in Order Management
- [ ] Admin confirms order → Status "Confirmed"
- [ ] Confirm button hides after confirmation
- [ ] Admin assigns rider → Rider details added
- [ ] Rider sees order in Tasks tab
- [ ] Customer sees "Confirmed" with rider details
- [ ] Rider updates to "Picked Up"
- [ ] Customer sees status update immediately
- [ ] Rider updates to "On the Way"
- [ ] Customer sees "On the Way" status
- [ ] Rider updates to "Delivered"
- [ ] Customer sees "I have received" button
- [ ] Customer clicks button → Status "Received"
- [ ] Feedback screen opens automatically
- [ ] Customer submits review → Status "Delivered Successfully"
- [ ] Order shows as completed with checkmark

