# Quick Reference Card - BuyNGo Order System

## The 9-Step Complete Flow

```
1. CUSTOMER ADDS ITEMS
   Shop → Click Product → Enter Qty → Add to Cart
   ✓ Multiple items supported
   ✓ Same product increments quantity
   
2. CUSTOMER PLACES ORDER  
   Cart → Checkout → Confirm
   ✓ Order saved to Firebase
   ✓ Status: "Pending"
   
3. CUSTOMER VIEWS ORDERS
   Orders tab
   ✓ Shows ALL customer's orders (not just 1)
   ✓ Real-time updates
   
4. ADMIN CONFIRMS
   Admin Dashboard → Confirm button
   ✓ Status: "Pending" → "Confirmed"
   ✓ Button LOCKED (hidden)
   
5. ADMIN ASSIGNS RIDER
   Admin Dashboard → Assign Rider button
   ✓ Pick rider from list
   ✓ Status: "Confirmed" → "Awaiting Pickup"
   ✓ Button LOCKED (hidden)
   
6. RIDER SEES ORDER
   Rider Dashboard
   ✓ Shows assigned orders
   ✓ Status: "Awaiting Pickup"
   ✓ "UPDATE STATUS" button clickable
   
7. RIDER UPDATES STATUS
   Click UPDATE STATUS → Progress through statuses
   ✓ Awaiting Pickup → Picked Up
   ✓ Picked Up → On the Way
   ✓ On the Way → Delivered
   ✓ Each saves to Firebase
   
8. CUSTOMER RECEIVES ORDER
   When status = "Delivered"
   ✓ Click "I have received the order"
   ✓ Status: "Delivered" → "Received"
   ✓ Review screen opens (mandatory)
   
9. CUSTOMER ADDS REVIEW
   Rate & Comment → Submit Review
   ✓ Status: "Received" → "Delivered Successfully"
   ✓ Review saved to Firebase
   ✓ Order COMPLETE ✓
```

---

## Status Values Used

### Admin Orders See
- **Pending** - Customer just placed
- **Confirmed** - Admin approved
- *(No rider-stage statuses visible to admin)*

### Rider Orders See  
- **Awaiting Pickup** - Initial (go get order)
- **Picked Up** - Have the package
- **On the Way** - Going to customer
- **Delivered** - Arrived at customer

### Customer Orders See
- **Pending** - Waiting for admin
- **Confirmed** - Admin approved it
- **Awaiting Pickup** - Rider getting it
- **Picked Up** - Rider has package
- **On the Way** - Rider heading here
- **Delivered** - Rider arrived
- **Received** - You marked received
- **Delivered Successfully** - Review submitted

---

## Key Buttons & Locks

### Admin Has These
```
✓ CONFIRM button
  └─ Only on "Pending" orders
  └─ Click → Status "Confirmed", button DISAPPEARS
  
✓ ASSIGN RIDER button  
  └─ Only on "Confirmed" orders
  └─ Click → Status "Awaiting Pickup", button DISAPPEARS
```

### Rider Has These
```
✓ UPDATE STATUS button
  └─ On each assigned order
  └─ Opens screen with status workflow
  
✓ MARK AS PICKED UP button
  └─ Enabled when status="Awaiting Pickup"
  └─ Disabled otherwise (grayed out)
  
✓ MARK AS ON THE WAY button
  └─ Enabled when status="Picked Up"
  └─ Disabled otherwise
  
✓ MARK AS DELIVERED button
  └─ Enabled when status="On the Way"
  └─ Disabled otherwise
```

### Customer Has These
```
✓ I have received the order button
  └─ Only when status="Delivered"
  └─ Click → Review screen opens
  
✓ Add Review button
  └─ When status="Received"
  └─ Fills in rating + comment
```

---

## Firebase Nodes

```
/orders/ORD-[timestamp]
  ├─ orderId: "ORD-[timestamp]"
  ├─ customerId: "[UID]" ← IMPORTANT for filtering
  ├─ customerName: "John Doe"
  ├─ items: {"Sugar": 5, "Milk": 3}
  ├─ totalAmount: 45.50
  ├─ status: "Pending|Confirmed|Awaiting Pickup|..."
  ├─ assignedRiderEmail: "rider@email.com"
  ├─ riderName: "Ahmed"
  └─ timestamps

/reviews/ORD-[timestamp]
  ├─ rating: 5
  ├─ comment: "Great service!"
  └─ timestamp: [ts]
```

---

## Real-Time Listeners

```
Customer's Order View:
  Listens to → /orders
  Filters → where customerId = my ID
  Updates → automatically when rider changes status
  
Rider's Dashboard:
  Listens to → /orders
  Filters → where assignedRiderEmail = my email
  Updates → automatically when new order assigned
  
Admin's Management:
  Listens to → /orders
  Shows → ALL orders
  Updates → automatically
```

---

## Testing Accounts

```
Customer:
  Email: cus@buyngo.com
  Password: password123
  
Admin:
  Email: admin@buyngo.com
  Password: password123
  
Rider:
  Email: abc@buyngo.com
  Password: password123
```

---

## Common Issues Quick Fix

| Issue | Check |
|-------|-------|
| Only see 1 order | Verify customerId in Firebase matches |
| Orders disappear | Confirm saved to Firebase not just local |
| Buttons grayed out | Check migration ran, status correct |
| Buttons reappear | Check button visibility logic |
| Can't add 2nd item | Check CartStore.addToCart() logic |
| Real-time not updating | Verify ValueEventListener listening |
| Admin can't assign | Confirm order first |
| Rider can't see order | Verify assignedRiderEmail matches |

---

## Logcat Debug Keywords

```
D/CusTracking - Customer order tracking
D/AdmOrderMgmt - Admin order management
D/RidDashboard - Rider dashboard
D/FirebaseRiderRepository - Rider operations
D/FirebaseOrderCleanup - Migrations
D/CartStore - Cart operations
```

---

## Key Concepts

### Cart (Local Only)
- Stored in SharedPreferences
- Cleared after checkout
- Supports multiple items
- Increments quantity for same product

### Orders (Firebase)
- Stored permanently
- Include customerId (for filtering)
- Status progresses through workflow
- Real-time listener for updates

### Workflows (3 Different)
- **Admin**: Pending → Confirmed
- **Rider**: Awaiting Pickup → Picked Up → On Way → Delivered
- **Customer**: Watches all stages + can review

### Transitions
- Admin assigns → Status changes from "Confirmed" to "Awaiting Pickup"
- This triggers rider workflow
- No status change back to "Confirmed"

### Buttons Lock After Use
- Admin CONFIRM → button gone forever
- Admin ASSIGN → button gone forever
- Can't undo or re-do these actions

---

## One-Minute Test

```
1. Add item to cart → Add another item → Go to Cart ✓
2. Checkout → See order in Firebase ✓
3. Switch to Admin → Confirm order → See button gone ✓
4. Admin Assign Rider → Button gone, status changed ✓
5. Switch to Rider → See order, click Update Status ✓
6. Mark as Picked Up → Button enables for next ✓
7. Switch to Customer → See status updated ✓
SUCCESS ✓
```

---

## Build Command

```bash
cd c:\Users\Administrator\OneDrive\Desktop\HDSE\MAD\MADcwgit\MAD-CW
.\gradlew clean assembleDebug
```

Expected: `BUILD SUCCESSFUL in ~10s`

---

## Remember

✅ **Cart** = Local (SharedPreferences)
✅ **Orders** = Firebase (permanent, synced)
✅ **Real-Time** = ValueEventListener (automatic updates)
✅ **Buttons** = Lock after use (can't undo)
✅ **Status** = Progresses one-way (can't go backwards)
✅ **Rider** = Only sees their assigned orders
✅ **Customer** = Only sees their own orders
✅ **Admin** = Sees all orders

---

## Next Steps

1. ✓ Read README_READY.md (overview)
2. ✓ Follow COMPLETE_FLOW_GUIDE.md (step-by-step)
3. ✓ Use TEST_PLAN.md (verify each step)
4. ✓ Reference CART_SYSTEM.md (if cart issues)
5. ✓ Use SYSTEM_SUMMARY.md (for architecture)

**Go build something awesome! 🚀**
