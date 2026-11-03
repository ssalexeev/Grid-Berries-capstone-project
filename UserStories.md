# GBerries - User Stories & System Requirements

## Overview of Epics
- **Epic 1:** Authentication & Access Control
- **Epic 2:** Product & Service Catalog
- **Epic 3:** Order Processing & Lifecycle
- **Epic 4:** User Activity Metrics & Recommendations
- **Epic 5:** AI-Powered Sales Analytics
- **Epic 6:** Administration & System Moderation

---

## Epic 1: Authentication & Access Control

### US1.1 - User Registration
* **User Story:** As a prospective user, I want to register an account as either a **Buyer** or a **Seller**, so that I can access role-specific platform features.
* **Key Functionality:** Account creation with role specification (`Buyer` or `Seller`).

### US1.2 - Secure Authentication (JWT)
* **User Story:** As a registered user, I want to log in with my credentials to receive a JWT access and refresh token, so that my sessions are secure and authenticated across API requests.
* **Key Functionality:** Credential validation, JWT token generation (Access & Refresh tokens).

### US1.3 - Seller Profile Management
* **User Story:** As a Seller, I want to update my store profile (business name, description, contact information), so that buyers can identify my brand.
* **Key Functionality:** Updating store details and contact metadata.

---

## Epic 2: Product & Service Catalog

### US2.1 - Product/Service Listing Management (CRUD)
* **User Story:** As a Seller, I want to create, update, view, and delete my product cards (including title, description, price, available stock quantity, categories, and image URLs), so that I can manage my inventory.
* **Key Functionality:** Complete CRUD operations for inventory product listings.

### US2.2 - Catalog Search & Filtering
* **User Story:** As a Buyer, I want to search for products/services by keyword and filter them by category, price range, and availability, so that I can quickly find items I want to buy.
* **Key Functionality:** Full-text search and multi-criteria filtering.

### US2.3 - Product Detail View
* **User Story:** As a Buyer, I want to open a product card to view its full details, stock level, and category tags, so that I can make an informed purchasing decision.
* **Key Functionality:** Product detail page rendering with real-time stock levels.

### US2.4 - Put Product In Cart
* **User Story:** As a Buyer, I want to add products to my cart, delete items from my cart, and update quantity parameters, so that I can prepare an order.
* **Key Functionality:** Shopping cart management (Add, Remove, Update Quantity).

---

## Epic 3: Order Processing & Lifecycle

### US3.1 - Order Placement
* **User Story:** As a Buyer, I want to place an order for one or more items, so that the system reserves stock and creates a new order in `Created` status.
* **Key Functionality:** Order initialization, stock reservation, status transition to `Created`.

### US3.2 - Order Status Management
* **User Story:** As a Seller, I want to view all incoming orders containing my products and transition their status (`Created` → `In Processing` → `Shipped` → `Completed` / `Cancelled`), so that I can fulfill orders effectively.
* **Key Functionality:** Order fulfillment tracking and status state transitions.

### US3.3 - Order Tracking
* **User Story:** As a Buyer, I want to view my order history and current order status, so that I can track my purchases in real time.
* **Key Functionality:** Order history listing and status tracking.

---

## Epic 4: User Activity Metrics & Recommendations

### US4.1 - Event Metric Tracking
* **User Story:** As the System, I want to automatically record user interaction events (`VIEW`, `SEARCH`, `ADD_TO_CART`, `PURCHASE`), so that customer behavior data can be aggregated for analytics and recommendations.
* **Key Functionality:** Automated event logging and telemetry collection.

### US4.2 - Personalized Recommendations
* **User Story:** As a Buyer, I want to see a "Recommended for You" feed on the storefront, so that I can discover products matched to my viewing and purchasing history.
* **Key Functionality:** Personalized product feed generation based on tracked user events (includes `US4.1`).

---

## Epic 5: AI-Powered Sales Analytics

### US5.1 - Sales Metrics Aggregation
* **User Story:** As a Seller, I want to view a dashboard displaying revenue, unit sales, conversion rates (views to purchases), and order cancellation rates over a selected date range.
* **Key Functionality:** Aggregated analytics dashboard (revenue, units sold, conversion rate, cancellation rate).

### US5.2 - AI Sales Insights & Recommendations
* **User Story:** As a Seller, I want to request an AI-generated analysis of my store metrics, so that I can receive actionable recommendations on pricing adjustments, inventory restocks, and underperforming products.
* **Key Functionality:** LLM-based store metric analysis (includes `US5.1`).

---

## Epic 6: Administration & System Moderation

### US6.1 - Category Tree Management
* **User Story:** As an Admin, I want to create, update, and organize product and service categories, so that the catalog maintains a clean hierarchy.
* **Key Functionality:** Category hierarchy and tree structure management.

### US6.2 - Seller & Product Moderation
* **User Story:** As an Admin, I want to review, approve, or suspend product listings and seller accounts, so that the platform complies with quality and safety standards.
* **Key Functionality:** Moderation workflow for accounts and product listings (Approve, Suspend, Review).