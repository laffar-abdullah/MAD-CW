# Complete Order Flow Test Plan

## Overview
This test verifies the complete order lifecycle from customer placement to rider delivery and customer review.

---

## TEST CASE 1: Customer Places Multiple Orders

### Precondition
- Customer logged into app (email: cus@buyngo.com or similar)
- Start with empty cart

### Steps
1. Go to CusHomeActivity (shop)
2. Add Item 1: **2x Sugar (1kg)** → Click "Add to Cart"
3. Add Item 2: **5x Milk** → Click "Add to Cart"
4. Go to **Cart** (click bottom nav)
5. Click **"CHECKOUT"** button
6. Fill delivery address (pre-filled from profile)
7. Click **"CONFIRM ORDER"**
8. **Verify**: Toast says "Order placed successfully!"
9. **Verify**: Cart is empty
10. Redirected to CusHomeActivity

### Repeat Steps for 2nd and 3rd Order
- Place 2nd order with different items
- Place 3rd order with different items
- **Total**: 3 orders in Firebase for this customer

### Expected Result
✅ All 3 orders saved to Firebase at `/orders/ORD-[timestamp]`
✅ Each order has `customerId` field matching the logged-in customer ID
✅ Each order has `status: "Pending"`

---

## TEST CASE 2: Customer Views All Orders (Before Logout)

### Precondition
- Customer still logged in after placing orders

### Steps
1. Go to CusTrackingActivity (Click nav_reviews or swipe)
2. **Observe** orders list

### Expected Result
✅ **All 3 orders appear** in the tracking screen
✅ Each shows: Order #, Status: Pending, Items list
✅ Status shows: "Waiting for admin confirmation..."
✅ No "I have received" button (status is Pending, not Delivered)

### If Issue
- ❌ See "No orders to track"
- Check Logcat for: `⊘ Skipping order (different customer)` 
- This means customerId doesn't match - order wasn't saved with proper customerId

---

## TEST CASE 3: Customer Logout and Login - Orders Persist

### Steps
1. From CusTrackingActivity, tap Profile (nav_profile)
2. Scroll down, click **"LOGOUT"**
3. **Verify**: Redirected to CusLoginActivity
4. Login with **same credentials** (email: cus@buyngo.com, password)
5. Go to CusTrackingActivity (Cart icon → Reviews or nav)

### Expected Result
✅ **All 3 orders still appear** (orders persisted in Firebase)
✅ Each order shows same status: Pending
✅ Orders are NOT lost after logout/login

### If Issue
- ❌ See "No orders to track"
- This means orders weren't saved to Firebase with correct customerId
- Check Logcat: `Total orders in Firebase: [X]` should show 3

---

## TEST CASE 4: Admin Confirms and Assigns Rider

### Precondition
- Switch to **Admin account** (email: admin@buyngo.com or similar)
- 3 customer orders are Pending in Firebase

### Steps
1. Open AdmOrderManagementActivity
2. **Verify**: See 3 pending orders in list
   - Order #ORD-[timestamp1], Status: Pending, "CONFIRM" + "ASSIGN RIDER" buttons visible
   - Order #ORD-[timestamp2], Status: Pending, "CONFIRM" + "ASSIGN RIDER" buttons visible
   - Order #ORD-[timestamp3], Status: Pending, "CONFIRM" + "ASSIGN RIDER" buttons visible

#### For Each Order:
1. Click **"CONFIRM"** button
2. **Verify**: Button disappears (status becomes "Confirmed")
3. **Verify**: Refreshing page - "CONFIRM" button stays hidden
4. Click **"ASSIGN RIDER"** button
5. Select rider (e.g., abc@buyngo.com)
6. Click **"CONFIRM ASSIGN"**
7. **Verify**: Toast says "Order assigned successfully"
8. **Verify**: Redirected back to admin list
9. **Verify**: "ASSIGN RIDER" button is now **HIDDEN** (can't re-assign)

### Expected Result - Firebase
✅ Order status: `"Confirmed"` → `"Awaiting Pickup"` (after assignment)
✅ Order has: `assignedRiderEmail: "abc@buyngo.com"`
✅ Order has: `riderName: "Rider Name"`
✅ Admin buttons locked (confirm and assign buttons hidden)

---

## TEST CASE 5: Rider Sees Assigned Orders on Dashboard

### Precondition
- Switch to **Rider account** (email: abc@buyngo.com)
- Admin has assigned 3 orders to this rider

### Steps
1. Open RidDashboardActivity
2. **Verify**: Section "Assigned Tasks"
3. **Expected to see**: Orders from Test Case 4

### What Happens Behind Scenes
- RidDashboardActivity.onResume() calls `FirebaseOrderCleanup.migrateConfirmedOrdersToAwaitingPickup()`
- This changes any "Confirmed" orders (assigned to this rider) to "Awaiting Pickup"
- This ensures old orders (before fix) also work

### Expected Result
✅ See **at least 1 order** in "Assigned Tasks" section
✅ Order shows:
   - Order #ORD-[timestamp]
   - Customer: [Name], Address: [Address]
   - Status: Awaiting Pickup (or "Confirmed" - fallback will work)
✅ "UPDATE STATUS" button is clickable

---

## TEST CASE 6: Rider Updates Order Status

### Precondition
- Rider logged in, viewing assigned order on dashboard

### Steps
1. Click **"UPDATE STATUS"** button on order card
2. Opens RidStatusUpdateActivity
3. **Verify**: Shows "Current status: Awaiting Pickup" (or "Confirmed" with fallback)
4. **Verify**: Button "MARK AS PICKED UP" is **ENABLED** (not grayed out)
5. Click **"MARK AS PICKED UP"**
6. **Verify**: Toast "Status updated to: Picked Up"
7. **Verify**: Dashboard refreshes, order status shows "Picked Up"
8. Click **"UPDATE STATUS"** again
9. **Verify**: "MARK AS ON THE WAY" is now enabled
10. Continue for each status:
    - "MARK AS ON THE WAY" → Status: "On the Way"
    - "MARK AS DELIVERED" → Status: "Delivered"

### Expected Result - Firebase
✅ Order status progresses: "Awaiting Pickup" → "Picked Up" → "On the Way" → "Delivered"
✅ After "Delivered", no more buttons shown (order complete from rider perspective)

---

## TEST CASE 7: Customer Sees Real-Time Delivery Status

### Precondition
- Switch back to **Customer account**
- Rider is currently updating order statuses

### Steps
1. Go to CusTrackingActivity
2. **Observe**: Refresh manually or wait for real-time update
3. **Verify**: Order status changes as rider updates
   - Initially: "Pending" or "Confirmed"
   - After rider marks: "Awaiting Pickup" → "Picked Up" → "On the Way" → "Delivered"

### Expected Result
✅ Customer sees real-time status updates
✅ When status = "Delivered":
   - "I have received the order" button appears
   - "Add Review" button NOT visible yet

---

## TEST CASE 8: Customer Marks Order Received and Adds Review

### Precondition
- Rider has marked order as "Delivered"
- Customer is viewing order in CusTrackingActivity

### Steps
1. Click **"I have received the order"** button
2. **Verify**: Toast says "Order received! Please add a review."
3. **Verify**: Redirected to CusFeedbackActivity
4. Fill review:
   - Rating: Select 1-5 stars
   - Comment: Type review text
5. Click **"SUBMIT REVIEW"**
6. **Verify**: Toast says "Review submitted successfully"
7. **Verify**: Redirected back to CusTrackingActivity

### Expected Result - Firebase
✅ Order status changes: "Delivered" → "Received" (when customer clicks button)
✅ After review submitted: "Received" → "Delivered Successfully"
✅ Review saved to `/reviews/[orderId]` with rating and comment

---

## TEST CASE 9: Order Marked Complete

### Precondition
- Customer has submitted review

### Steps
1. View CusTrackingActivity again
2. **Verify**: Order shows "✓ Order Delivered Successfully"
3. **Verify**: No more buttons (order fully closed)

### Expected Result
✅ Order lifecycle complete
✅ Order cannot be modified further
✅ Status is final: "Delivered Successfully"

---

## LOGCAT Debug Checks

### When Customer Places Order
```
Expected:
D/CusCheckout: Order placed: ORD-[timestamp] with customerId: [uid]
D/Firebase: Order saved successfully
```

### When Customer Views Orders
```
Expected:
D/CusTracking: Total orders in Firebase: 3
D/CusTracking: Current customer ID: [uid]
D/CusTracking: Found order: ORD-[timestamp1], customerId: [uid], status: Pending
D/CusTracking: ✓ Displaying order: ORD-[timestamp1]
D/CusTracking: ✓ Found 3 orders for customer: [uid]
```

### When Admin Assigns Rider
```
Expected:
D/AdmOrderMgmt: Order [timestamp] confirmed and button hidden
D/FirebaseRiderRepository: ✓ Order assigned successfully to rider: abc@buyngo.com with status: Awaiting Pickup
```

### When Rider Views Dashboard
```
Expected:
D/RidDashboard: Refreshing task for rider email: abc@buyngo.com
D/FirebaseOrderCleanup: ✓ Migrated order [timestamp] from 'Confirmed' to 'Awaiting Pickup'
D/RidDashboard: Found order for rider: ORD-[timestamp]
```

---

## Issue Resolution Checklist

If customer sees only 1 order:
- [ ] Check if all 3 orders have correct `customerId` in Firebase
- [ ] Check Logcat: `ordersFound` count should be 3
- [ ] Verify customer placed orders while logged in (not as guest)

If orders disappear after logout/login:
- [ ] Verify orders saved to Firebase (not just local storage)
- [ ] Check `customerId` field matches logged-in user's UID
- [ ] Verify Firebase real-time database rules allow read access

If rider can't update status:
- [ ] Check if status was properly set to "Awaiting Pickup"
- [ ] Look for fallback working: "Confirmed" → treated as "Awaiting Pickup"
- [ ] Verify migration ran: Check Logcat for `Migrated order`
- [ ] Check buttons are not grayed out (alpha = 1.0)

If rider dashboard shows "No active tasks":
- [ ] Verify admin assigned order to correct rider email
- [ ] Check Firebase: `assignedRiderEmail` matches rider's email
- [ ] Verify order has `status: "Awaiting Pickup"` (not "Confirmed")
- [ ] Check `getAssignedOrdersForRider()` finds the order

---

## Success Criteria

✅ **TEST PASSES IF:**
1. Customer can place 3 orders
2. All 3 orders visible in tracking (don't logout/login)
3. Orders persist after logout/login
4. Admin can confirm orders (button disappears)
5. Admin can assign riders (button disappears after assignment)
6. Rider sees assigned orders on dashboard
7. Rider can update order status through all stages
8. Customer sees real-time status updates
9. Customer can mark received and add review
10. Order shows "Delivered Successfully" after review

---

## Build Status
✅ BUILD SUCCESSFUL (15s)

Ready to test! 🚀
