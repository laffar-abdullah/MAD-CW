ackage com.example.buyngo.Model;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 *                            PRODUCT MODEL FILE
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * WHAT THIS FILE DOES:
 * This file defines what a product (grocery item) looks like. It contains basic
 * info like name, price, category, description, and availability.
 * 
 * HOW IT CONNECTS TO FIREBASE:
 * 1. Admin creates products in Firebase Console under /products/{productId}
 * 2. CusHomeActivity loads all products from Firebase when app opens
 * 3. App converts Firebase data into Product objects
 * 4. Displays products on home screen as cards (name, price, image)
 * 5. When customer taps "Add to Cart", this Product data is saved to CartStore
 * 6. CusCheckoutActivity later uses product price to calculate total
 * 
 * HOW CUSTOMER MODULE USES IT:
 * - CusHomeActivity: Loads and displays all products from Firebase
 * - CusProductDetailActivity: Shows detailed view when customer taps a product
 * - CusCartActivity: Stores product info (name, price) for items in cart
 * - CusCheckoutActivity: Uses price to calculate order total
 * 
 * DATA FLOW:
 * Firebase /products/ → CusHomeActivity loads → Display product cards
 *                             ↓
 *                    Customer taps "Add to Cart"
 *                             ↓
 *                    Product saved to CartStore (local phone storage)
 *                             ↓
 *                    CusCartActivity displays cart with product names/prices
 *                             ↓
 *                    Checkout calculates total using prices
 * 
 * IMPORTANT FIELDS:
 * - id: Unique product identifier
 * - name: Product name (e.g., "Basmati Rice")
 * - category: Type of product (e.g., "Grains", "Oil", "Sugar")
 * - price: Cost per unit in Pakistani Rupees (Rs.)
 * - description: What the product is (shown on detail screen)
 * - imageUrl: URL to product image (loaded from Firebase or image CDN)
 * - stock: How many units available
 * 
 * NOTE: Products are READ-ONLY for customers. Customers only READ products
 * from Firebase, they don't create or modify them.
 * ═══════════════════════════════════════════════════════════════════════════════
 */
public class Product {
    private String id;
    private String name;
    private String category;
    private double price;
    private String description;
    private String imageUrl;
    private int stock;

    public Product() {
    }

    public Product(String id, String name, String category, double price, String description, String imageUrl, int stock) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
        this.imageUrl = imageUrl;
        this.stock = stock;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    @Override
    public String toString() {
        return "Product{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", price=" + price +
                ", description='" + description + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", stock=" + stock +
                '}';
    }
}
