# Geetanjali Tailor Shop — Student Viva Examination Study Notes

This document provides a concise, structured breakdown of all features built in **Stage 2** of the Geetanjali Tailor Shop web application. It is specifically designed for quick revision before viva exams and technical project demonstrations.

---

## 📚 Stage 2 Features Reference

---

### Feature 1: Order Number Formatting & Dynamic Balance Calculation (Step 1)

* **What the User Sees**:
  * Human-readable sequence order numbers formatted as `TS-YYYY-NNNN` (e.g., `TS-2026-0014`) on the Order Detail header, Home recent orders table, and Order Creation success screen.
  * Status balance chips: Green **"Paid"** chip when balance is 0, or Red **"Balance Rs. 400"** chip when balance remains.
  * Financial summary on Order Detail: Total Stitching Cost, Total Paid, and Remaining Balance formatted as `Rs. 1,500` without trailing decimals.
* **Files Involved**:
  * [`OrderNumberFormatter.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/util/OrderNumberFormatter.java): Formats sequence strings and currency.
  * [`CustomerOrder.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/entity/CustomerOrder.java): Pure arithmetic getters (`getTotal()`, `getPaid()`, `getBalance()`, `isPaid()`).
  * Templates: `orders/detail.html`, `home/index.html`, `orders/wizard-done.html`.
* **Execution Flow (Step-by-Step)**:
  1. Browser requests Order Detail page via `GET /orders/{id}`.
  2. `OrderController` loads `CustomerOrder` from database via `OrderService`.
  3. In Thymeleaf, `${order.balance}` triggers calculated method `CustomerOrder.getBalance()`.
  4. `CustomerOrder` iterates its `items` list to compute `total` (sum of item prices) and its `payments` list to compute `paid` (sum of payment amounts).
  5. `CustomerOrder` subtracts `paid` from `total` using `BigDecimal.subtract()`.
  6. Thymeleaf evaluates `${@orderNumberFormatter.format(order)}` and `${@orderNumberFormatter.formatMoney(order.balance)}` to render the formatted badges.
* **Tables Read & Written**:
  * **Read**: `customer_order`, `order_item`, `payment`.
  * **Written**: None (balance is pure runtime calculation; zero writes).

---

### Feature 2: Payments Section & Add Payment Modal (Step 2)

* **What the User Sees**:
  * Chronological list of payments under Order Detail (oldest first: `#1 (Advance)`, `#2`, etc.) showing payment date, payment mode badge (CASH or UPI), and amount.
  * An **"Add Payment"** button that opens a native HTML5 `<dialog>` modal with amount, Cash/UPI toggle, and payment date fields.
  * Instant feedback with green flash toast on success or red error message on business rule violation.
* **Files Involved**:
  * [`PaymentForm.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/dto/form/PaymentForm.java): Form DTO with Bean Validation constraints.
  * [`PaymentService.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/service/PaymentService.java): Enforces positive amount, balance cap, and date rules.
  * [`OrderController.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/controller/OrderController.java): Validates `returnTo` against open redirects and handles POST request.
  * Template: `orders/detail.html`.
* **Execution Flow (Step-by-Step)**:
  1. User fills `<dialog>` form and clicks Save, submitting `POST /orders/{id}/payments`.
  2. `OrderController.addPayment(...)` validates form fields using `@Valid`.
  3. Controller delegates to `PaymentService.addPayment(id, form)`.
  4. `PaymentService` loads the order, checks that `amount > 0`, verifies `amount <= order.getBalance()`, and ensures `paidOn <= today`.
  5. `PaymentService` saves the new `Payment` entity into PostgreSQL inside `@Transactional`.
  6. Controller validates `returnTo` (must start with single `/` and not `//`), flashes success message, and redirects.
* **Tables Read & Written**:
  * **Read**: `customer_order`, `order_item`, `payment`.
  * **Written**: `payment` (inserts 1 new payment row).

---

### Feature 3: Item Status Workflow & Mark Done (Step 3)

* **What the User Sees**:
  * Lifecycle status chip next to every garment item: `PENDING` (yellow), `DONE` (blue), or `DELIVERED` (purple).
  * A **"Mark Done"** button next to any item in `PENDING` status.
  * Button automatically vanishes once the item is marked `DONE`.
* **Files Involved**:
  * [`ItemStatus.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/enums/ItemStatus.java): Enum (`PENDING`, `DONE`, `DELIVERED`).
  * [`OrderItem.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/entity/OrderItem.java): `markDone(Instant now)` domain logic method.
  * [`OrderService.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/service/OrderService.java): `@Transactional markDone` service method.
  * [`OrderController.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/controller/OrderController.java): Maps `POST /orders/{id}/items/{itemId}/done`.
  * Template: `orders/detail.html`.
* **Execution Flow (Step-by-Step)**:
  1. Tailor clicks "Mark Done" on an item, sending `POST /orders/{id}/items/{itemId}/done`.
  2. `OrderController` calls `OrderService.markDone(orderId, itemId)`.
  3. `OrderService` fetches `OrderItem` from `OrderItemRepository` inside a transaction.
  4. `OrderItem.markDone(clock.instant())` verifies that current status is `PENDING` (throws `BusinessRuleException` otherwise).
  5. `OrderItem` sets status to `DONE` and stamps `done_at = clock.instant()`.
  6. Controller flashes success message and redirects back to `/orders/{id}` (PRG pattern).
* **Tables Read & Written**:
  * **Read**: `order_item`.
  * **Written**: `order_item` (updates `status` = 'DONE', `done_at` = current timestamp).

---

### Feature 4: Delivery Block & Owner Override (Step 4)

* **What the User Sees**:
  * **"Mark Delivered"** button available on items that are `PENDING` or `DONE`.
  * If the order has an unpaid balance (`balance > 0`): delivery is blocked.
    * **Tailor**: Sees red panel: *"Delivery is blocked because payment is pending. Ask the owner to approve."* No form or button displayed.
    * **Owner**: Sees red panel with reason input box (required, max 500 chars) and **"Override and deliver"** button.
  * If owner submits override, item delivers and permanently displays: *"Delivered with override: <reason>"*.
  * If order balance is 0: item delivers normally with zero warnings or overrides.
* **Files Involved**:
  * [`BalancePendingException.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/exception/BalancePendingException.java): Custom exception carrying outstanding amount.
  * [`OverrideForm.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/dto/form/OverrideForm.java): DTO with `@NotBlank` and `@Size(max = 500)` validation.
  * [`DeliveryService.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/service/DeliveryService.java): Encapsulates delivery rules and atomic audit logging.
  * [`SecurityConfig.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/security/SecurityConfig.java): Enforces `hasRole("OWNER")` on `/orders/*/items/*/override`.
  * [`OrderController.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/controller/OrderController.java): Captures delivery POST requests and redirects.
  * Template: `orders/detail.html`.
* **Execution Flow (Step-by-Step)**:
  1. User clicks "Mark Delivered", invoking `POST /orders/{id}/items/{itemId}/deliver`.
  2. `DeliveryService.deliverItem(orderId, itemId)` checks `order.getBalance()`. If `balance > 0`, it throws `BalancePendingException(balance, itemId)`.
  3. `OrderController` catches `BalancePendingException`, flashes `blockedItemId` and `blockedBalance`, and redirects to `/orders/{id}`.
  4. In `orders/detail.html`, if `currentUser.role == 'OWNER'`, the owner override form is shown.
  5. Owner submits reason to `POST /orders/{id}/items/{itemId}/override`. SecurityConfig checks role; `DeliveryService` re-checks `currentUser.isOwner()` in Java code.
  6. Inside one `@Transactional` method, `DeliveryService` saves a `DeliveryOverride` record and sets item status to `DELIVERED` with `delivered_at`.
* **Tables Read & Written**:
  * **Read**: `customer_order`, `order_item`, `payment`, `app_user`.
  * **Written**: `delivery_override` (inserts 1 row), `order_item` (updates `status` = 'DELIVERED', `delivered_at`).

---

### Feature 5: Pending Payments Ledger & Dashboard Metrics (Step 5)

* **What the User Sees**:
  * Dedicated dashboard page at `GET /payments/pending`:
    * Large banner displaying total shop-wide outstanding money (`Total Pending: Rs. 1,000`).
    * Table listing only orders with remaining balance > 0, sorted by earliest delivery date (oldest first).
    * Dynamic status chips: `"delivered N days ago"`, `"due in N days"`, or `"overdue by N days"`.
    * **"Payments"** action button opening that order's `#payments-section`.
  * Home dashboard card: *"Pending Rs. 9,500, View"* linking to `/payments/pending` (hidden when total balance is 0).
  * Customer detail profile: Metric tile showing customer's cumulative outstanding balance.
* **Files Involved**:
  * [`PendingPaymentRow.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/dto/PendingPaymentRow.java): DTO record for individual table rows.
  * [`PendingPaymentsSummary.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/dto/PendingPaymentsSummary.java): Container holding total outstanding and rows list.
  * [`PaymentService.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/service/PaymentService.java): Calculates rows, filters, and computes status text.
  * [`PaymentController.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/controller/PaymentController.java): Serves `GET /payments/pending`.
  * [`HomeController.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/controller/HomeController.java): Passes `pendingTotal` and `hasPendingBalance`.
  * [`CustomerController.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/controller/CustomerController.java): Computes customer total outstanding.
  * Templates: `payments/pending.html`, `home/index.html`, `customers/detail.html`.
* **Execution Flow (Step-by-Step)**:
  1. User navigates to `GET /payments/pending`.
  2. `PaymentController` calls `PaymentService.getPendingPaymentsSummary()`.
  3. `PaymentService` loads orders and filters where `order.getBalance() > 0`.
  4. For each unpaid order, `computeDueText()` checks if any item was delivered (`"delivered N days ago"`), or computes remaining days (`"due in N days"` / `"overdue by N days"`).
  5. Rows are sorted in Java by earliest delivery date (ascending order).
  6. `payments/pending.html` renders the total outstanding metric and rows table.
* **Tables Read & Written**:
  * **Read**: `customer_order`, `order_item`, `payment`, `customer`.
  * **Written**: None (read-only analytical ledger).

---

## 🎯 8 Essential Viva Questions & Concise Answers

### Q1: Why is the balance calculated dynamically on the fly instead of stored in the database?
> **Answer**: Storing calculated fields in a database violates database normalization and introduces data inconsistency risks (dual source of truth). If payments or order items are added, edited, or deleted, a stored balance column can easily drift out of sync. Calculating balance on the fly (`Total Items Price - Total Payments Received`) guarantees 100% mathematical accuracy at all times with zero synchronization bugs.

### Q2: Why is the delivery block enforced at the order level rather than the item level?
> **Answer**: In tailor shop businesses, advance deposits are paid against the whole customer order, not earmarked for specific shirts or trousers. If a customer ordered 3 garments for Rs. 1,500 and paid Rs. 500 advance, allowing them to collect the first shirt would let them walk away with completed clothes without guaranteeing payment for the rest. Enforcing the rule at the order level ensures the entire order is financially settled before physical goods leave the shop.

### Q3: Why do we use a dedicated `delivery_override` audit table?
> **Answer**: Delivering goods when money is pending is a financial risk and a business exception. A dedicated audit table ensures non-repudiation and transparency by recording exactly *who* authorized the release (`overridden_by`), *how much* money was pending at that exact instant (`balance_at_time`), *why* it was approved (`reason`), and *when* it occurred. This prevents disputes and theft.

### Q4: Why is the owner role check performed on the server and not just by hiding the UI button?
> **Answer**: Hiding a button in the browser (client-side) is purely cosmetic and provides zero actual security. Any user or attacker can open Developer Tools or send a crafted HTTP POST request using `curl` or Postman directly to `/orders/{id}/items/{itemId}/override`. Enforcing security on the server via `SecurityConfig` and programmatic role checks in `DeliveryService` guarantees authorization cannot be bypassed.

### Q5: Why is `BigDecimal` used for currency instead of `double` or `float`?
> **Answer**: `double` and `float` are binary floating-point representations based on IEEE 754, which cannot accurately represent base-10 fractions (e.g. `0.1 + 0.2 = 0.30000000000000004`). In financial systems, accumulated rounding errors cause money discrepancies. `BigDecimal` provides arbitrary-precision fixed-point arithmetic with deterministic rounding modes, ensuring exact rupee and paise calculations.

### Q6: What does `@Transactional` do in `DeliveryService.deliverWithOverride()`?
> **Answer**: `@Transactional` ensures atomicity across multiple database write operations. When an override occurs, two things must happen together: (1) an audit row must be inserted into `delivery_override`, and (2) the item status must be updated to `DELIVERED`. If the database crashes or an exception occurs halfway through, `@Transactional` automatically rolls back all changes, ensuring the database is never left in a corrupted or half-updated state.

### Q7: What is an open redirect vulnerability, and how did we prevent it in `returnTo`?
> **Answer**: An open redirect occurs when an application accepts an untrusted destination URL parameter (like `returnTo=http://evil-phishing.com`) and blindly redirects the user to it, allowing attackers to trick users into phishing sites using a trusted domain. We prevent this by strictly validating `returnTo`: it must start with a single `/`, and cannot start with `//` (protocol-relative external redirect) or contain `http`. If invalid, the app safely defaults to `/orders/{id}`.

### Q8: Why do we use the Post/Redirect/Get (PRG) pattern after POST requests?
> **Answer**: If a browser submits a POST form and the server directly returns an HTML view, refreshing the page (F5) causes the browser to pop up a confirmation alert and re-submit the POST request. This leads to duplicate operations, such as charging duplicate payments or recording duplicate items. The PRG pattern instructs the browser to redirect (HTTP 302/303) to a GET endpoint after processing the POST, so refreshing simply reloads the view harmlessly.
