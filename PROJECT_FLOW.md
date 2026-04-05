# Project Flow 

## Main Features
1. **Sign Up** - Create account
2. **Shop** - Browse and add items to cart
3. **Checkout** - Place order with payment
4. **Track** - See delivery status in real-time
5. **Feedback** - Rate delivery (MAIN FEATURE)


CUSTOMER 

1. SIGNUP
   Customer opens app
   Fills email, password, name, phone, address
   Creates Firebase account
   Profile saved to database

2. SHOPPING  
   Home screen loads
   See all products from Firebase
   Select quantity for each item
   Add to cart (saved locally on phone)

3. CHECKOUT
   Go to cart
   Review all items
   Enter delivery address
   Choose payment method (Card or Cash)
   Confirm order
   Order saved to Firebase

4. TRACKING
   Go to My Orders
   See order status in real-time
   Admin assigns rider
   Rider updates status as delivery progresses
   Customer sees updates instantly

5. FEEDBACK (MAIN FEATURE)
   After delivery
   Rate 1-5 stars
   Add comment (optional)
   Submit feedback
   OR go to Reviews menu anytime for anonymous feedback


ADMIN 

1. LOGIN
   Admin enters email and password
   Firebase verifies credentials
   Logs into dashboard

2. PRODUCT MANAGEMENT
   Add new product
   Enter name, price, category, description, stock
   Save to database
   
   Edit existing product
   Update any field
   Changes applied immediately
   
   Delete product
   Remove from system

3. ORDER MANAGEMENT
   View all orders in list
   Filter by status
   See customer details and items
   Assign available rider to order
   Order status updated to Assigned

4. RIDER MANAGEMENT
   Register new rider
   Fill email, password, name, phone
   Create account
   View all riders with ratings

5. FEEDBACK MONITORING
   View all customer feedback
   See ratings and comments
   Check which riders getting low ratings
   Monitor delivery quality


RIDER 

1. LOGIN
   Rider enters email and password
   Firebase verifies
   Logs into dashboard

2. VIEW ORDERS
   See list of orders assigned to me
   Click order to see details
   View customer address and items

3. UPDATE DELIVERY STATUS
   Order assigned to me
   Status shows Order Placed
   
   I pick up order from store
   Status changes to Picked Up
   
   I start driving to customer
   Status changes to On the Way
   
   I deliver to customer
   Status changes to Delivered
   
   Customer sees all updates in real-time

4. DELIVERY HISTORY
   View completed deliveries
   See total orders delivered
   Check earnings

5. VIEW FEEDBACK
   See customer ratings about my deliveries
   Read comments from customers
   View overall rating score
   Track feedback over time
```

---

## Database Structure 

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

