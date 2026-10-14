# Geetanjali Tailor Shop Web Application (Stage 2 Wrap-Up)

A production-ready, clean-architecture tailor-shop management web application built with **Java 21**, **Spring Boot 4.1.1**, **Thymeleaf**, **Spring Data JPA**, **PostgreSQL 17**, **Flyway**, and **Spring Security**.

---

## 🛠️ Technology Stack
- **Java**: 21 (LTS)
- **Framework**: Spring Boot 4.1.1
- **Database**: PostgreSQL 17 (Flyway schema migrations `V1`, `V2`, `V3`)
- **Template Engine**: Thymeleaf + Material Design 3 (Material You MD3)
- **Security**: Spring Security (Role-based access: `ROLE_OWNER`, `ROLE_TAILOR`)
- **Build Tool**: Maven Wrapper (`./mvnw`)

---

## 🔑 Default Login Credentials
| Role | Mobile Number | Password | Capabilities |
| :--- | :--- | :--- | :--- |
| **Owner** | `9876543210` | `secretpassword` | Full shop access, settings, measurements, orders, payments, **delivery overrides** |
| **Tailor** | `7894561230` | `sumit` | Measurements, orders, stitch status (`markDone`), **blocked from delivery override** |

---

## 🌟 Stage 2 Features Summary

1. **Order Number & Balance Maths**:
   - Order numbers formatted as `TS-<year>-<4-digit order_no>` (e.g. `TS-2026-0014`) using the Asia/Kolkata Clock zone.
   - Dynamic arithmetic getters on `CustomerOrder`: `getTotal()`, `getPaid()`, `getBalance()`, and `isPaid()`. Balance is strictly calculated on the fly and never stored in the database.
   - Dynamic status chips (Green `"Paid"` vs. Red `"Balance Rs. 400"`) formatted without decimals for whole numbers (`Rs. 1,500`).

2. **Payments Section & Settlement**:
   - Chronological payment receipts list (oldest first) showing payment number, date, mode (Cash/UPI), and amount.
   - Interactive `<dialog>` modal to record additional payments (`POST /orders/{id}/payments`).
   - Strict server-side validation: amount must be positive, cannot exceed remaining balance, and date cannot be in the future.
   - Open redirect protection: `returnTo` URLs must strictly begin with a single `/` and never `//` or external schemes.

3. **Item Lifecycle Status (`PENDING` -> `DONE` -> `DELIVERED`)**:
   - Each garment item maintains status chips: `PENDING` (yellow), `DONE` (blue), or `DELIVERED` (purple).
   - "Mark Done" action (`POST /orders/{id}/items/{itemId}/done`) transitionable strictly from `PENDING`.

4. **Delivery Block & Owner Override (Core Feature)**:
   - "Mark Delivered" action (`POST /orders/{id}/items/{itemId}/deliver`) allowed from `PENDING` or `DONE`.
   - **Financial Protection Rule**: If an order has any unpaid balance (`balance > 0`), delivery is blocked by throwing `BalancePendingException`.
   - **Tailor Experience**: Displays an informational warning panel: *"Delivery blocked: Balance Rs. X pending. Ask the owner to approve."* No override controls shown.
   - **Owner Experience**: Displays a reason textarea (mandatory, max 500 chars) and an "Override and deliver" button.
   - **Defense-in-Depth**:
     - URL filter in `SecurityConfig`: `.requestMatchers("/orders/*/items/*/override").hasRole("OWNER")`.
     - Programmatic check in `DeliveryService.deliverWithOverride(...)` throwing `AccessDeniedException` if caller is not an owner.
   - **Audit Trail**: Every override atomically persists a row in `delivery_override` (`order_item_id`, `balance_at_time`, `reason`, `overridden_by`) in the same database transaction.
   - Overridden items show a permanent audit note on the order detail page: *"Delivered with override: <reason>"*.

5. **Pending Payments Ledger & Dashboard Metrics**:
   - Dedicated dashboard at `GET /payments/pending`:
     - Total shop-wide outstanding money banner at the top.
     - Chronological list of orders with `balance > 0`, sorted by earliest promised delivery date (oldest first).
     - Intelligent status labels: `"delivered N days ago"`, `"due in N days"`, or `"overdue by N days"`.
     - Quick-action button linking directly to each order's payments section (`/orders/{id}#payments-section`).
   - Home Dashboard Card: *"Pending Rs. 9,500, View"* banner linking to `/payments/pending` (rendered only when total outstanding > 0).
   - Customer Profile: Displays customer's total outstanding balance across their order history.

---

## 🚀 Stage 2 End-to-End Demo Script

Follow these exact steps to demonstrate all Stage 2 features:

1. **Sign in as Owner**:
   - Visit `http://localhost:8080/login`.
   - Log in with Mobile: `9876543210`, Password: `secretpassword`.

2. **Create Order with Unpaid Balance**:
   - Click **New Order** (`/orders/new`).
   - Select or create a customer, add a garment item priced at **Rs. 1,500** with delivery date.
   - In Step 4 (Review & Bill), enter an advance payment of **Rs. 500** and submit.
   - **Verify**: The order-saved screen shows order number formatted like `TS-2026-XXXX`, and a red chip: `Balance Rs. 1,000`.

3. **Mark Stitched (`DONE`)**:
   - On the Order Detail page, click **Mark Done** on the item.
   - **Verify**: Item status changes from `PENDING` to blue `DONE`.

4. **Attempt Delivery as Owner (Delivery Blocked)**:
   - Click **Mark Delivered** on the item.
   - **Verify**: Delivery is blocked because `Rs. 1,000` is pending.
   - A red warning panel appears showing the pending balance, a reason box, and an **Override and deliver** button.

5. **Verify Tailor Cannot Override**:
   - Sign out (`POST /logout`).
   - Sign in as **Tailor**: Mobile `7894561230`, Password `sumit`.
   - Navigate to the same order (`/orders/{id}`).
   - Click **Mark Delivered**.
   - **Verify**: Red panel displays *"Delivery is blocked because payment is pending. Ask the owner to approve."* No text box or override button is provided.

6. **Perform Owner Override**:
   - Sign back in as **Owner** (`9876543210`).
   - Navigate to the order, click **Mark Delivered**.
   - Enter reason: *"Loyal customer - will pay remaining 1000 via UPI on Friday"*.
   - Click **Override and deliver**.
   - **Verify**: Item badge updates to purple `DELIVERED`, and an audit note appears below it:
     *"Delivered with override: Loyal customer - will pay remaining 1000 via UPI on Friday"*.

7. **Verify Database Audit Row in PostgreSQL**:
   - Inspect the database table:
     ```sql
     SELECT * FROM delivery_override ORDER BY id DESC LIMIT 1;
     ```
   - **Verify**: A row exists with `order_item_id`, `balance_at_time = 1000.00`, `reason`, and `overridden_by = 1`.

8. **Create Second Order & Pay in Full**:
   - Create a second order with total **Rs. 800** and **Rs. 400** advance deposit.
   - On the Order Detail page, click **Add Payment**.
   - Enter amount **Rs. 400**, select UPI, and click Save.
   - **Verify**: The balance chip turns green **"Paid"**, and Remaining Balance shows **Rs. 0**.

9. **Deliver Fully Paid Order Without Override**:
   - Click **Mark Delivered** on the second order's item.
   - **Verify**: Item delivers instantly without any red warning panel or override audit note.

10. **Verify Pending Payments Dashboard**:
    - Click **Dashboard** (`/`) and observe the red banner card: *"Pending Rs. 1,000, View"*.
    - Click **View** to open `/payments/pending`.
    - **Verify**:
      - Total outstanding at the top shows `Rs. 1,000`.
      - Only the first order (with balance) is listed; the second fully paid order is excluded.
      - The status chip says `"delivered 0 days ago"`.
      - Clicking **Payments** scrolls directly to `#payments-section`.

---

## 🧪 Automated Testing
Run the complete unit and integration test suite:
```bash
./mvnw test
```
All **139 tests** pass cleanly with 0 failures and 0 errors.
