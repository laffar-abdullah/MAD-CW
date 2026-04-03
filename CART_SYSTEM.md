# Cart System - Quick Verification

## How the Cart Works

### Storage
- Cart items stored in **SharedPreferences** (local device storage)
- Uses JSON serialization via Gson
- Key: `cart_preferences` / `cart_items`

### Adding Items
1. User selects product and quantity
2. `CartStore.addToCart()` is called
3. **Logic**:
   - Checks if productId already exists in cart
   - If EXISTS: increment quantity (e.g., 2 + 3 = 5)
   - If NEW: add as new item
4. Item saved back to SharedPreferences

### Displaying Cart
- `CusCartActivity` calls `CartStore.getCartItems()`
- Deserializes JSON back to List<CartItem>
- Displays each item with quantity and price

### Checkout
- `CusCheckoutActivity` calls `CartStore.getCartItems()`
- Creates Order object with all items
- Saves to Firebase `/orders/ORD-[timestamp]`
- Clears cart via `CartStore.clearCart()`

---

## Test Case: Adding Multiple Items

### Add Item 1
```
Product: Sugar (1kg)
Quantity: 2
Action: Click "Add to Cart"
Result: CartStore finds no Sugar in cart → adds new item with qty=2
Toast: "2 x Sugar (1kg) added to cart!"
Storage: {"productId": "sugar1kg", "name": "Sugar (1kg)", "quantity": 2}
```

### Add Item 2
```
Product: Milk
Quantity: 5
Action: Click "Add to Cart"
Result: CartStore finds no Milk in cart → adds new item with qty=5
Toast: "5 x Milk added to cart!"
Storage: [Sugar item, Milk item]
```

### Add Item 1 AGAIN (Same Product)
```
Product: Sugar (1kg) 
Quantity: 3
Action: Click "Add to Cart"
Result: CartStore finds Sugar in cart → INCREMENTS qty (2 + 3 = 5)
Toast: "3 x Sugar (1kg) added to cart!"
Storage: [{"Sugar", qty: 5}, {"Milk", qty: 5}]
```

### View Cart
```
Screen: CusCartActivity
Display:
  - 5 x Sugar (1kg)  $[5 × price]
  - 5 x Milk        $[5 × price]
Total: $[sum]
```

---

## Code Flow - Adding to Cart

```
CusHomeActivity
  ↓ (click product)
CusProductDetailActivity
  ↓ (enter qty, click Add)
CartStore.addToCart() 
  ↓
  Check if product exists in SharedPreferences
  ├─ YES: Increment quantity
  └─ NO: Add new item
  ↓
  Save to SharedPreferences
  ↓
Toast + finish()
Back to CusHomeActivity
```

---

## Code Flow - Checkout

```
CusCartActivity
  ↓ (click CHECKOUT)
CusCheckoutActivity
  ↓ (fill address, click CONFIRM)
CartStore.getCartItems()
  ↓ (deserialize from SharedPreferences)
Create Order object with:
  - orderId: "ORD-" + timestamp
  - customerId: current user UID
  - customerName: from Firebase users
  - items: Cart items as Map<String, Integer>
  - totalAmount: sum of (price × qty)
  - status: "Pending"
  ↓
Save to Firebase: /orders/ORD-[timestamp]
  ↓
CartStore.clearCart()
  ↓
Toast + navigate to CusHomeActivity
```

---

## Logcat Debug Messages

### When Adding First Item
```
CartStore.addToCart() called
- productId: sugar1kg
- name: Sugar (1kg)
- quantity: 2
→ Item not found in cart, adding new
→ Saved to SharedPreferences
Toast: "2 x Sugar (1kg) added to cart!"
```

### When Adding Same Product Again
```
CartStore.addToCart() called
- productId: sugar1kg  
- name: Sugar (1kg)
- quantity: 3
→ Item FOUND in cart with qty=2
→ Incrementing to qty=5
→ Saved to SharedPreferences
Toast: "3 x Sugar (1kg) added to cart!"
```

### When Viewing Cart
```
CusCartActivity.displayCartItems()
→ CartStore.getCartItems()
→ Deserialized 2 items:
  1. Sugar (1kg), qty=5, price=$1.00
  2. Milk, qty=5, price=$0.80
→ Total: $9.00
```

### When Placing Order
```
CusCheckoutActivity.createAndSaveOrder()
→ CartStore.getCartItems()
→ Build items map:
  {"Sugar (1kg)": 5, "Milk": 5}
→ totalAmount: $9.00
→ Save to Firebase /orders/ORD-[timestamp]
→ CartStore.clearCart()
→ Toast: "Order placed successfully!"
```

---

## Common Issues & Fixes

### Issue: "Can only add 1 item, can't add 2nd item"
**Possible Causes**:
1. Cart activity crashes (check for exceptions)
2. CartStore throws error (check Logcat)
3. Product IDs not unique (check product data)
4. SharedPreferences permission issue

**Debug**:
- Check Logcat for crashes
- Add logging to CartStore.addToCart()
- Verify product IDs are different for different products

### Issue: "Items don't appear in cart"
**Possible Causes**:
1. Toast appeared but items not saved
2. displayCartItems() not retrieving items
3. JSON deserialization failing

**Debug**:
- Check Logcat: `CusCartActivity.displayCartItems()`
- Verify items saved: Print cartItems.size()
- Check SharedPreferences in Settings

### Issue: "Same item added twice shows only 1 instead of qty=4"
**Possible Causes**:
1. Product IDs are not matching (different IDs for same product)
2. CartStore.addToCart() not finding existing item
3. Quantity not incrementing properly

**Debug**:
- Verify productId is same both times
- Add logging: `Log.d("CartStore", "Found existing: " + item.productId)`
- Check increment math: `item.quantity += quantity`

### Issue: "Cart persists after logout (should be fresh)"
**Expected Behavior**: Cart is LOCAL storage, stays until cleared
**If Issue**: Doesn't clear on checkout
- Check: `CartStore.clearCart()` is called
- Verify SharedPreferences.Editor.clear() works
- Check: `addOnSuccessListener` fires before clearCart

---

## Quick Test Commands

### Clear Cart Manually
```
Settings → Storage → Clear Data → BuyNGo (or via ADB)
Or: CartStore.clearCart() in code
```

### View SharedPreferences
```
ADB: adb shell "run-as com.example.buyngo cat shared_prefs/cart_preferences.xml"
```

### Check Firebase Orders
```
Firebase Console → Realtime Database → orders → [orderId]
Should see:
{
  "orderId": "ORD-1234567890",
  "customerId": "[uid]",
  "customerName": "Customer Name",
  "items": {
    "Sugar (1kg)": 5,
    "Milk": 5
  },
  "totalAmount": 9.00,
  "status": "Pending"
}
```

---

## Summary

✅ **Cart System Supports**:
- Adding multiple different items
- Incrementing quantity when same item added twice
- Persisting cart in local storage
- Clearing cart after checkout
- Displaying all items with totals

✅ **Order System Stores**:
- customerId (links to customer)
- All cart items as a map
- Total amount
- Initial status: "Pending"

✅ **Flow Works If**:
1. Can add 2+ items to cart
2. Cart shows all items
3. Checkout creates Firebase order
4. Order visible in CusTrackingActivity for customer

