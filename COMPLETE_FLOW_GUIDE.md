# Complete Order Flow - Step by Step Guide

## 📋 Overview
This guide walks through the ENTIRE order lifecycle:
1. Customer adds multiple items to cart
2. Customer places order
3. Order appears in customer's orders (as Pending)
4. Admin confirms the order
5. Admin assigns rider to order
6. Rider sees order on dashboard
7. Rider marks order status updates (Picked Up → On Way → Delivered)
8. Customer sees real-time updates
9. Customer marks received and adds review
10. Order closes as "Delivered Successfully"

---

## 🎯 STEP 1: Customer Adds Multiple Items to Cart

### Login as Customer
- Email: `cus@buyngo.com` (or create one if first time)
- Password: `password123` (or your password)
- Navigate to **CusHomeActivity** (Shop)

### Add First Item
1. Scroll and find a product (e.g., **Sugar (1kg)**)
2. Tap the product card → Opens **CusProductDetailActivity**
3. Enter quantity: `2`
4. Click **"Add to Cart"** button
5. **Verify**: Toast shows "2 x Sugar (1kg) added to cart!"
6. Activity closes → Back to shop

### Add Second Item
1. Find another product (e.g., **Milk**)
2. Tap the product card
3. Enter quantity: `5`
4. Click **"Add to Cart"**
5. **Verify**: Toast shows "5 x Milk added to cart!"
6. Activity closes → Back to shop

### Add Third Item (SAME product to test increment)
1. Find **Sugar (1kg)** again
2. Tap the product card
3. Enter quantity: `3`
4. Click **"Add to Cart"**
5. **Verify**: Toast shows "3 x Sugar (1kg) added to cart!"
6. **Important**: CartStore will INCREMENT the quantity (2 + 3 = 5 total)

---

## 🛒 STEP 2: Review Cart and Place Order

### View Cart
1. Click bottom nav **"Cart"** (nav_cart icon - currently shows "Add" icon)
2. **CusCartActivity** opens
3. **Verify Cart Contents**:
   - 5 x Sugar (1kg)  — $[price x 5]
   - 5 x Milk         — $[price x 5]
   - Total: $[sum]
4. Verify all 3 items are there (Sugar quantity incremented)

### Checkout
1. Click **"CHECKOUT"** button
2. **CusCheckoutActivity** opens with order summary
3. **Verify**: Shows all items and total price
4. **Delivery Address Section** (pre-filled from profile):
   - Phone: [number]
   - Address: [address]
   - City: [city]
5. If any field is empty, fill them
6. Click **"CONFIRM ORDER"** button
7. **Verify**: Toast says "Order placed successfully!"
8. Cart clears
9. Redirected to **CusHomeActivity**

### What Happened
- Order saved to Firebase at `/orders/ORD-[timestamp]`
- Order contains: orderId, customerId (YOUR ID), customerName, items (map), totalAmount, status: "Pending"
- Cart cleared from local storage (SharedPreferences)

---

## 📊 STEP 3: Customer Views Placed Order as Pending

### View Your Orders
1. From shop, click bottom nav **"Orders"** (nav_orders icon)
2. **CusTrackingActivity** opens
3. **Verify Orders List**:
   - Shows your order: "Order #ORD-[timestamp]"
   - Status: **Pending**
   - Items: "5x Sugar (1kg), 5x Milk"
   - Message: "Waiting for admin confirmation..."
   - NO "I have received" button (status is Pending, not Delivered)

### Logout Test (OPTIONAL - Verify Persistence)
1. Go to Profile (nav_profile)
2. Scroll down and click **"LOGOUT"**
3. Login again with same credentials
4. Go to Orders again
5. **Verify**: Order STILL shows (not lost after logout)

---

## 👨‍💼 STEP 4: Admin Confirms Order

### Login as Admin
- Logout from customer account first
- Email: `admin@buyngo.com` (or your admin account)
- Password: `password123`

### View Pending Orders
1. Navigate to **AdmOrderManagementActivity**
2. **Verify**: See the customer's order
   - Order #ORD-[timestamp]
   - Customer: [Customer Name]
   - Items: 5x Sugar (1kg), 5x Milk | Total: $[sum]
   - Status: **Pending**
   - Buttons: "CONFIRM" (green) + "ASSIGN RIDER" (gray/disabled or less prominent)

### Confirm the Order
1. Click **"CONFIRM"** button
2. **Verify**: Toast says "Order Confirmed!"
3. **Verify**: "CONFIRM" button **DISAPPEARS** (becomes GONE permanently)
4. Status now shows: **Confirmed**
5. Refresh screen (back and forth) - "CONFIRM" button should stay hidden

---

## 🚗 STEP 5: Admin Assigns Rider to Order

### Assign Rider
1. With confirmed order visible, click **"ASSIGN RIDER"** button
2. **AdmAssignRiderActivity** opens with rider list
3. Shows:
   - Order #ORD-[timestamp]
   - Customer: [Name]
   - Available riders (e.g., `abc@buyngo.com - Rider Name`)

### Select Rider
1. Click on a rider radio button (e.g., `abc@buyngo.com`)
2. Click **"CONFIRM ASSIGN"** button
3. **Verify**: Toast says "Order assigned successfully"
4. Activity closes → Back to admin order list

### Verify Assignment
1. **Verify**: "ASSIGN RIDER" button now **DISAPPEARS** (can't re-assign)
2. Status now shows: **Awaiting Pickup** (transitioned from "Confirmed")
3. Order is locked - no changes possible

### What Happened
- Order now has: `assignedRiderEmail: "abc@buyngo.com"`, `riderName: "Rider Name"`
- Status changed: "Confirmed" → "Awaiting Pickup" (rider workflow starts)
- Both admin buttons locked (can't modify further)

---

## 🚙 STEP 6: Rider Views Assigned Order on Dashboard

### Login as Rider
- Logout from admin account
- Email: `abc@buyngo.com` (the email admin assigned to)
- Password: `password123`

### View Dashboard
1. Navigate to **RidDashboardActivity**
2. **Behind Scenes**: `migrateConfirmedOrdersToAwaitingPickup()` runs automatically
3. **Verify "Assigned Tasks" Section**:
   - Shows: "Order #ORD-[timestamp]"
   - Customer: [Name]
   - Address: [Address]
   - Status: **Awaiting Pickup** (or "Confirmed" with fallback logic)
   - Green button: "UPDATE STATUS"

### Click UPDATE STATUS
1. Click the **"UPDATE STATUS"** button
2. **RidStatusUpdateActivity** opens
3. **Verify**:
   - Displays: "Order: #ORD-[timestamp]"
   - Displays: "Current status: Awaiting Pickup"
   - **"MARK AS PICKED UP"** button is **ENABLED** (not grayed out)
   - Other buttons ("MARK AS ON THE WAY", "MARK AS DELIVERED") are DISABLED (grayed out)

---

## 📍 STEP 7: Rider Updates Order Status Through All Stages

### Stage 1: Mark as Picked Up
1. Click **"MARK AS PICKED UP"** button
2. **Verify**: Toast says "Status updated to: Picked Up"
3. Return to dashboard (button disappears if status complete)
4. Re-enter UPDATE STATUS screen
5. **Verify**: 
   - Status now shows: "Picked Up"
   - "MARK AS PICKED UP" button now **DISABLED** (grayed out)
   - **"MARK AS ON THE WAY"** button now **ENABLED**

### Stage 2: Mark as On the Way
1. Click **"MARK AS ON THE WAY"** button
2. **Verify**: Toast says "Status updated to: On the Way"
3. Check status: Now shows "On the Way"
4. **Verify Buttons**:
   - "MARK AS ON THE WAY" → DISABLED
   - **"MARK AS DELIVERED"** → ENABLED

### Stage 3: Mark as Delivered
1. Click **"MARK AS DELIVERED"** button
2. **Verify**: Toast says "Status updated to: Delivered"
3. Status now shows: "Delivered"
4. Return to dashboard
5. **Verify**: Order no longer appears in "Assigned Tasks" (completed)

---

## 👥 STEP 8: Customer Sees Real-Time Status Updates

### Watch Status Change
(Do this while rider is updating status above)

1. Login as customer again
2. Go to **CusTrackingActivity** (Orders)
3. Your order shows current status
4. **Refresh** screen manually (pull-to-refresh if available, or back/forth)
5. **Verify Status Updates in Real-Time**:
   - Starts as: "Awaiting Pickup"
   - Changes to: "Picked Up"
   - Changes to: "On the Way"
   - Changes to: "Delivered"

### Key Point
- Each time rider updates, Firebase updates
- Customer's app listens via ValueEventListener
- Status automatically reflects latest value

---

## ✅ STEP 9: Customer Marks Order Received

### Receive Order
1. Verify order status is now: **"Delivered"**
2. **Verify Button**: Green "I have received the order" button appears
3. Click the button
4. **Verify**: Toast says "Order received! Please add a review."
5. Redirected to **CusFeedbackActivity**

### What Happened
- Order status changed: "Delivered" → "Received"
- Review screen opens (now mandatory)

---

## ⭐ STEP 10: Customer Adds Review

### Add Review
1. **CusFeedbackActivity** is open
2. Shows order details and review form
3. **Fill Review**:
   - Rating: Click stars to select 1-5 rating (e.g., 5 stars)
   - Comment: Type review text (e.g., "Great delivery service!")
4. Click **"SUBMIT REVIEW"** button
5. **Verify**: Toast says "Review submitted successfully"
6. Redirected back to **CusTrackingActivity**

### What Happened
- Order status changed: "Received" → "Delivered Successfully"
- Review saved to Firebase at `/reviews/[orderId]`
- Order now fully closed

### Verify Final Status
1. View order in **CusTrackingActivity**
2. **Verify**: Order shows "✓ Order Delivered Successfully"
3. **Verify**: No more buttons (fully closed, read-only)

---

## 🔍 Verification Checklist

### After Checkout
- [ ] Order appears in Firebase at `/orders/ORD-[timestamp]`
- [ ] Order has correct `customerId`
- [ ] Status is "Pending"
- [ ] All items are saved in the map

### After Admin Confirms
- [ ] Status changes to "Confirmed"
- [ ] "CONFIRM" button disappears from admin view
- [ ] Button stays hidden after refresh

### After Admin Assigns
- [ ] Order gets `assignedRiderEmail` and `riderName`
- [ ] Status changes to "Awaiting Pickup"
- [ ] "ASSIGN RIDER" button disappears
- [ ] Button stays hidden after refresh

### After Rider Sees Order
- [ ] Order appears on RidDashboardActivity
- [ ] Status shows as "Awaiting Pickup"
- [ ] "UPDATE STATUS" button is clickable

### After Each Rider Status Update
- [ ] Firebase has latest status
- [ ] Customer sees update in real-time
- [ ] Next status button becomes enabled

### After Customer Marks Received
- [ ] Status changes to "Received"
- [ ] Review screen opens
- [ ] Can submit review

### After Review Submitted
- [ ] Status changes to "Delivered Successfully"
- [ ] Order shows checkmark
- [ ] No more buttons
- [ ] Review saved in Firebase

---

## 📱 Testing Multiple Orders

### Repeat for 2 More Orders
1. Login as **different customer** (create new account)
2. Add items to cart → Place order
3. Or add more items as same customer → Place second order
4. Login as **admin**
5. Confirm both orders
6. Assign to **SAME rider** (e.g., abc@buyngo.com)
7. Login as rider
8. **Verify**: Dashboard shows **BOTH** orders
9. Update statuses on both

### Expected
- Rider can see multiple assigned orders
- Can update each independently
- Customer sees all their orders

---

## 🐛 Troubleshooting

### Customer Can't See Own Orders
- [ ] Check Logcat: `D/CusTracking: Total orders in Firebase: X`
- [ ] Verify customerId matches: `D/CusTracking: Current customer ID: [uid]`
- [ ] Check if orders show: `D/CusTracking: ✓ Found X orders for customer`
- **Fix**: Ensure order was saved with correct customerId when placed

### Rider Can't See Assigned Orders
- [ ] Check admin assigned to correct email
- [ ] Verify order has `assignedRiderEmail` in Firebase
- [ ] Verify status is "Awaiting Pickup" (or fallback to "Confirmed")
- [ ] Check Logcat: `D/RidDashboard: Found order for rider`

### Rider Buttons All Grayed Out
- [ ] Migration should have run: `D/FirebaseOrderCleanup: ✓ Migrated order`
- [ ] Fallback logic should have converted "Confirmed" to "Awaiting Pickup"
- [ ] Check status in Firebase matches expected value

### Order Disappears After Logout
- [ ] Orders saved to Firebase (not just local)
- [ ] Check `/orders` node in Firebase Console
- [ ] Verify order has customerId field

---

## ✅ Success Criteria - Full Flow Works If:

1. ✅ Customer adds 2+ different items to cart
2. ✅ Customer can increment quantity by adding same item twice
3. ✅ All cart items show on checkout
4. ✅ Order places successfully
5. ✅ Order appears as Pending in customer tracking
6. ✅ Order persists after logout/login
7. ✅ Admin sees order in management screen
8. ✅ Admin can confirm (button disappears)
9. ✅ Admin can assign rider (button disappears)
10. ✅ Rider sees order on dashboard
11. ✅ Rider can update status through all stages
12. ✅ Customer sees real-time status updates
13. ✅ Customer can mark received when status is Delivered
14. ✅ Customer can add review
15. ✅ Order shows "Delivered Successfully" after review

---

**Build Status**: ✅ BUILD SUCCESSFUL (10s)

🎉 **Ready to test the complete flow!**
