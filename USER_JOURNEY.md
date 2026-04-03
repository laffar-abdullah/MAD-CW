# User Journey & Feature Flow - BuyNGo Complete

## 1. CUSTOMER JOURNEY

### Phase 1: Shopping & Checkout
```
Customer Home
    ↓
Browse Products (all products displayed)
    ↓
Select Product → Quantity Input (default: 1)
    ↓
Click "Add" → Item added to cart
    ↓
Repeat for multiple items
    ↓
Click Cart → View all items with quantities
    ↓
Click "Checkout" → Enter delivery address
    ↓
Click "Confirm Order"
    ↓
Order Created with:
  - Order ID: ORD-{timestamp}
  - Items Map: {Product1: qty1, Product2: qty2, ...}
  - Total Amount: $calculated_sum
  - Status: Pending
  - Customer ID: {uid}
    ↓
Order sent to Firebase
    ↓
Cart cleared
    ↓
Redirect to Home
```

### Phase 2: Order Tracking
```
Customer clicks "Track Order"
    ↓
CusTrackingActivity loads
    ↓
Query Firebase: orders where customerId == current_user_uid
    ↓
For each order, display:
  - Order ID
  - Items: "2x Burger, 1x Fries" ✅ (Now showing correctly!)
  - Total: "$15.50" ✅ (Now showing correctly!)
  - Status with color coding:
    • Pending (Orange)
    • Confirmed (Orange)
    • Awaiting Pickup (Orange)
    • Picked Up (Yellow)
    • On the Way (Blue)
    • Delivered (Green)
    ↓
If Status == Pending:
  → Show "Waiting for admin confirmation..."
    ↓
If Status == Delivered:
  → Show "I have received the order" button (GREEN)
  → If rider info available:
    • Email: rider@example.com
    • Name: Rider Name
    • Delivery Status: (Picked Up, On the Way, Delivered)
```

### Phase 3: Receive Order & Optional Review
```
Status: Delivered
    ↓
Customer clicks "I have received the order" (GREEN BUTTON)
    ↓
Status changes to: "Received"
    ↓
List refreshes
    ↓
Two buttons appear side-by-side:
  [Add Review] [Skip]
    ↓
OPTION A: Customer clicks "Add Review"
  → Open CusFeedbackActivity
  → Form: Rating (1-5 stars) + Comment text
  → No mandatory fields (all optional)
  → Submit review
  → Status: "Delivered Successfully"
  → Green checkmark display
    ↓
OPTION B: Customer clicks "Skip"
  → Status: "Delivered Successfully"
  → Green checkmark display
  → No review submitted
    ↓
Final Status: "✓ Order Delivered Successfully" (GREEN)
```

---

## 2. ADMIN JOURNEY

### Phase 1: Order Management
```
Admin clicks "Order Management"
    ↓
AdmOrderManagementActivity loads
    ↓
Query Firebase: all orders (no filter)
    ↓
Display each order card:
  - Order ID
  - Customer Name
  - Items: "2x Burger, 1x Fries" ✅ (Now showing!)
  - Total: "$15.50" ✅ (Now showing!)
  - Status
  - Assigned Rider (if assigned)
```

### Phase 2: Two-Step Workflow - Assign THEN Confirm
```
For Pending Order (No Rider Assigned):
  ↓
Status: Pending
Rider: (empty)
  ↓
Show ONLY "Assign Rider" button (BLUE)
  ↓
Admin clicks "Assign Rider"
  → AdmAssignRiderActivity opens
  → List all available riders
  → Admin selects rider
  → Update Order.assignedRiderEmail = "rider@example.com"
  ↓
Return to Order Management
  ↓
Same order now shows:
  Status: Pending
  Rider: rider@example.com
  ↓
Show ONLY "Confirm" button (GREEN)
  ↓
Admin clicks "Confirm"
  → Order.status = "Confirmed"
  → Status changes to "Awaiting Pickup"
  → Both buttons hide
  ↓
Rider can now see order in their dashboard
```

### Phase 3: Order Management Tools
```
"Clear All" button in toolbar
    ↓
Admin clicks "Clear All"
    ↓
Confirmation dialog:
  "Are you sure you want to delete all orders?
   This action cannot be undone."
    ↓
  [Yes, Delete All] [Cancel]
    ↓
If confirmed → All orders deleted from Firebase
  → Refresh display
  → Show "No orders available"
```

---

## 3. RIDER JOURNEY

### Phase 1: Dashboard - View Assigned Orders
```
Rider logs in
    ↓
RidDashboardActivity loads
    ↓
Check: Is rider session active?
  → If NO: Redirect to RidLoginActivity
  → If YES: Continue
    ↓
Query Firebase: all orders where assignedRiderEmail == rider_email
    ↓
For each assigned order, display card:
  - Order ID
  - Customer Name
  - Items: "2x Burger, 1x Fries" ✅ (Now showing with logging!)
  - Total: "$15.50" ✅ (Now showing with logging!)
  - Status
  - "Update Status" button (if not Delivered)
    ↓
Compatibility Check:
  → FirebaseOrderCleanup.ensureOrdersHaveItemsList()
  → Automatically handles old orders
  → Logs for debugging
```

### Phase 2: Status Updates
```
For non-Delivered orders:
    ↓
Rider clicks "Update Status"
  → RidUpdateStatusActivity opens
  → Available transitions:
    • Awaiting Pickup → Picked Up
    • Picked Up → On the Way
    • On the Way → Delivered
    ↓
Rider selects new status
    ↓
Order.status = selected_status
    ↓
Return to dashboard
    ↓
Dashboard refreshes with new status
```

### Phase 3: Delivery History & More
```
Bottom navigation bar:
  [Tasks] [History] [Reviews] [Profile]
    ↓
Tasks: Current assigned orders (default)
History: Completed deliveries
Reviews: Customer feedback ratings
Profile: Rider information & settings
```

---

## 4. DATA FLOW DIAGRAM

### Order Object Structure (Firebase):
```json
{
  "orders": {
    "ORD-1775235978710": {
      "orderId": "ORD-1775235978710",
      "customerId": "uid123456",
      "customerName": "Laffar",
      "items": {},  // OLD FORMAT (empty map, not used)
      "itemsList": [  // NEW FORMAT (properly serialized)
        {"name": "Burger", "quantity": 2},
        {"name": "Fries", "quantity": 1}
      ],
      "totalAmount": 15.50,
      "status": "Delivered",
      "assignedRiderEmail": "rider@example.com",
      "riderName": "Ahmed",
      "createdAt": 1775235978710,
      "updatedAt": 1775235998710
    }
  }
}
```

### Order Serialization Process:
```
Java Map<String, Integer> items
  ↓
  ├─ "Burger" → 2
  ├─ "Fries" → 1
  ↓
  [Constructor: items → itemsList]
  ↓
ArrayList<OrderItem>
  ├─ OrderItem("Burger", 2)
  ├─ OrderItem("Fries", 1)
  ↓
  [Firebase.setValue(order)]
  ↓
  JSON in Firebase (itemsList properly serialized)
  ↓
  [Firebase.getValue(Order.class)]
  ↓
  Order object with itemsList populated
  ↓
  [order.getItems() called]
  ↓
  itemsList → converted back to Map
  ↓
  Display ready!
```

---

## 5. STATUS COLOR CODING

### Delivery Status Colors:
| Status | Color | Meaning |
|--------|-------|---------|
| Pending | Orange (#FF9800) | Waiting for admin confirmation |
| Confirmed | Orange | Admin confirmed, waiting for rider |
| Awaiting Pickup | Orange | Rider will pick up soon |
| Picked Up | Yellow (#FFC107) | Rider has the order |
| On the Way | Blue (#2196F3) | Rider is delivering |
| Delivered | Green (#4CAF50) | At customer location |
| Received | Green | Customer confirmed receipt |
| Delivered Successfully | Green ✓ | Order complete |

---

## 6. NEW FEATURES SUMMARY

### ✅ Multi-Item Cart
- Add multiple items at once
- Quantity input per product
- Cart shows all items with total

### ✅ Items & Total Display
- Admin sees all items: "2x Burger, 1x Fries"
- Rider sees all items: "2x Burger, 1x Fries"
- Customer sees all items: "2x Burger, 1x Fries"
- All show correct total: "$15.50"

### ✅ Admin Two-Step Workflow
- Step 1: Assign rider (button shows until assigned)
- Step 2: Confirm order (button shows after assigned)
- Clear all orders with confirmation

### ✅ Customer Received & Optional Review
- "I have received" button on Delivered orders
- Optional review submission (not mandatory)
- Skip option to close without review
- Final "✓ Order Delivered Successfully" status

### ✅ Comprehensive Logging
- Order creation logs items and total
- Rider dashboard logs item retrieval
- Admin logs items and total on display
- Debug-friendly console output

### ✅ Backward Compatibility
- Old orders without itemsList still work
- Automatic migration on load
- Graceful fallback to items map
- No data loss for existing orders

---

## 7. TESTING SCENARIOS

### Scenario A: Complete Happy Path
1. Customer adds 2x Burger + 1x Fries
2. Checkout shows total "$15.50"
3. Admin sees items displayed correctly
4. Admin assigns rider
5. Admin confirms order
6. Rider dashboard shows items & total
7. Rider updates status to Delivered
8. Customer clicks "I have received"
9. Customer adds review
10. Order shows "✓ Order Delivered Successfully"

### Scenario B: Skip Review Path
1. Steps 1-7 same as Scenario A
2. Customer clicks "Skip" instead of "Add Review"
3. Order shows "✓ Order Delivered Successfully"
4. No review data stored

### Scenario C: Multiple Orders
1. Create 3 different orders with different items
2. Admin page shows all 3 with correct items/totals
3. Rider assigned only to order #1 sees only order #1
4. Customer sees all 3 their orders

---

## 8. EDGE CASES HANDLED

| Edge Case | Handling |
|-----------|----------|
| No items in order | Displays "No items" |
| Total is $0 | Displays "$0.00" |
| Old orders without itemsList | Auto-converted on display |
| Null assignment fields | Treated as empty/unassigned |
| Customer creates duplicate orders | Each gets unique ID & tracked separately |
| Rider closes app mid-delivery | Next resume refreshes orders |
| Admin logs out & in | Orders persist with status & assignments |

