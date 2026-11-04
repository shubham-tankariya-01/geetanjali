# Geetanjali Tailoring Studio: System Architecture, Workflows & Database Schema (ERD)

> **Created By**: Shubham Tankariya & Sumit Tailor  
> **Technologies Used**: Java 21 | Spring Boot 4.1.1 | Spring Data JPA | PostgreSQL 17 | Flyway | Thymeleaf | Groq & Gemini AI  

---

## 1. High-Level System Architecture

Geetanjali is built as a complete web application that runs directly on the tailor shop's computer. We chose **Spring Boot with Java 21** and **PostgreSQL 17** because tailor shops need a system that is fast, reliable, and keeps customer records and money completely safe without crashes.

```mermaid
graph TD
    Client["Tailor / Owner Browser (Desktop or Tablet)"]
    
    subgraph WebLayer ["1. Web & Security Layer"]
        Security["Spring Security (Protects owner settings & passwords)"]
        Controllers["Web Controllers (Handles URLs and web pages)"]
    end
    
    subgraph ServiceLayer ["2. Business Logic Layer (The Brain)"]
        Services["Services (Calculates balance, checks workload, manages orders)"]
        AIOrchestrator["AI Assistant (Answers staff questions using Groq or Gemini)"]
    end
    
    subgraph DataLayer ["3. Database & Storage Layer"]
        SpringData["Spring Data JPA (Communicates with database)"]
        PhotoStore["Photo Storage (Saves cloth photos on disk)"]
    end
    
    subgraph Storage ["4. Physical Storage"]
        Postgres[("PostgreSQL 17 Database")]
        DiskUploads[("Cloth Photos Folder (/uploads)")]
    end
    
    Client --> Security
    Security --> Controllers
    Controllers --> Services
    Controllers --> AIOrchestrator
    Services --> SpringData
    Services --> PhotoStore
    SpringData --> Postgres
    PhotoStore --> DiskUploads
```

---

## 2. The 4 Layers of the Application Explained Simply

To keep the code clean and easy to maintain, the application is divided into 4 simple layers:

1. **Controllers (`controller/`) — The Reception Desk**:
   - Listens to web browser requests (like opening `/orders` or submitting a new customer form).
   - Checks if the input is valid (for example, making sure a mobile number has 10 digits).
   - Hands over the work to the right service.
   - Tells the browser which HTML page to show.

2. **Services (`service/`) — The Brain**:
   - Contains all the real business rules of the tailor shop.
   - Examples:
     - Calculating the outstanding balance on an order.
     - Checking if a tailor already has 5 or 10 orders on a date.
     - Blocking delivery if money is still unpaid.
   - When saving an order, it makes sure all parts (customer, items, measurements, advance payment) are saved together in one safe transaction. If anything fails, it cancels everything so data is never half-saved.

3. **Repositories (`repository/`) — The Librarian**:
   - Talks directly to PostgreSQL using Spring Data JPA.
   - Finds records quickly (like finding a customer by their mobile number, or counting how many orders are due on a specific day).

4. **Entities (`entity/`) — The Data Records**:
   - Java classes that represent the database tables (Customer, Order, Garment, Payment, etc.).

---

## 3. Database Schema & Entity Relationship Diagram (ERD)

The database has 11 simple, interconnected tables managed automatically by Flyway migrations:

```mermaid
erDiagram
    shop ||--o{ app_user : "employs"
    app_user ||--o{ customer_order : "books"
    app_user ||--o{ delivery_override : "approves"
    customer ||--o{ customer_order : "places"
    customer ||--o{ measurement : "has"
    garment ||--|{ garment_field : "has_fields"
    garment ||--o{ order_item : "is_type_of"
    garment ||--o{ measurement : "measured_for"
    measurement ||--|{ measurement_value : "contains"
    garment_field ||--o{ measurement_value : "specifies"
    customer_order ||--|{ order_item : "contains"
    customer_order ||--o{ payment : "has_payments"
    order_item ||--o{ item_photo : "attaches"
    order_item ||--o{ delivery_override : "audits"

    shop {
        bigint id PK
        varchar name "Shop Name (Geetanjali Tailoring)"
        varchar address "Shop Address"
        varchar phone "Contact Phone"
    }

    app_user {
        bigint id PK
        varchar full_name "Staff name"
        varchar mobile UK "Login mobile number"
        varchar password_hash "BCrypt encrypted password"
        varchar role "OWNER or TAILOR"
    }

    customer {
        bigint id PK
        varchar name "Customer full name"
        varchar mobile UK "10-digit mobile number"
    }

    customer_order {
        bigint id PK
        bigint order_no UK "Order sequence (e.g. 1004)"
        bigint customer_id FK "Customer who ordered"
        bigint created_by FK "Staff who booked it"
        timestamptz created_at "Order date and time"
    }

    order_item {
        bigint id PK
        bigint order_id FK "Parent order"
        bigint garment_id FK "Garment type (Shirt, Pant, etc.)"
        bigint measurement_id FK "Saved measurements snapshot"
        varchar notes "Special stitching instructions"
        numeric price "Price for stitching this item"
        date delivery_date "Promised completion date"
        varchar status "PENDING, DONE, or DELIVERED"
        timestamptz done_at "When stitching finished"
        timestamptz delivered_at "When customer picked it up"
    }

    delivery_override {
        bigint id PK
        bigint order_item_id FK "Item that was delivered"
        numeric balance_at_time "Unpaid balance when delivered"
        varchar reason "Why owner allowed delivery"
        bigint overridden_by FK "Owner who approved"
        timestamptz created_at "Time of approval"
    }

    garment {
        bigint id PK
        varchar name "Shirt, Pant, Kurta, Safari, etc."
        integer sort_order "Display order on screen"
        boolean active "Is this garment currently offered"
    }

    garment_field {
        bigint id PK
        bigint garment_id FK "Which garment this field belongs to"
        varchar name "Field key (chest, length, waist)"
        varchar label "Screen label (e.g. Chest / છાતી)"
        varchar unit "inches or cm"
        integer sort_order "Order when taking measurements"
        boolean is_required "Is this field mandatory"
    }

    measurement {
        bigint id PK
        bigint customer_id FK "Customer"
        bigint garment_id FK "Garment"
        timestamptz created_at "Snapshot creation time"
    }

    measurement_value {
        bigint measurement_id PK, FK "Measurement snapshot"
        bigint field_id PK, FK "Measurement field"
        numeric value "Measurement number (e.g. 38.50)"
    }

    payment {
        bigint id PK
        bigint order_id FK "Which order this payment is for"
        numeric amount "Amount paid (e.g. 500.00)"
        varchar mode "CASH or UPI"
        date payment_date "Date money was received"
    }

    item_photo {
        bigint id PK
        bigint order_item_id FK "Order item"
        varchar photo_url "URL to view the photo"
        varchar file_path "Saved path on computer disk"
    }
```

---

## 4. Main Everyday Workflows

### 4.1. Booking a New Order (4 Steps)

```mermaid
sequenceDiagram
    autonumber
    actor Tailor
    participant Screen as New Order Wizard
    participant System as Order Service
    participant Database as PostgreSQL

    Tailor->>Screen: Step 1: Type customer 10-digit mobile number
    Screen->>Database: Check if customer exists
    alt Existing Customer
        Database-->>Screen: Auto-fills customer name and past order count
    else New Customer
        Tailor->>Screen: Type new customer name
    end
    Tailor->>Screen: Step 2: Click garment tile (e.g. Shirt, Trouser)
    Tailor->>Screen: Step 3: Enter measurements & pick delivery date
    Screen->>System: Check how many orders are already on this date
    alt 10 or more orders already
        System-->>Tailor: Hard Block: "Maximum 10 orders reached, pick another date"
    else 5 to 9 orders
        System-->>Tailor: Alert: "This date already has N orders, confirm to add?"
        Tailor->>Screen: Clicks Yes to confirm
    else Under 5 orders
        System-->>Tailor: Green light: Date is available
    end
    Tailor->>Screen: Step 4: Enter advance money (Cash or UPI)
    Tailor->>Screen: Click 'Place Order'
    Screen->>Database: Safely saves order, items, measurements & payment
    Database-->>Tailor: Shows Order Confirmation screen with order number!
```

---

### 4.2. Stitched Garment Status: PENDING &rarr; DONE &rarr; DELIVERED

Every piece of clothing goes through 3 simple states:

1. **`PENDING` (Yellow)**: The order is placed. The cloth is waiting to be cut or is being stitched on the sewing machine.
2. **`DONE` (Blue)**: The tailor finished stitching and pressing. It is now hanging on the rack ready for customer trial.
3. **`DELIVERED` (Purple)**: The customer tried it on, paid the remaining balance, and took it home.

---

### 4.3. Delivery Protection & Balance Check Flow

To make sure the shop never loses money:

```mermaid
flowchart TD
    CustomerComes[Customer arrives to pick up clothes] --> OpenOrder[Tailor opens order on screen]
    OpenOrder --> CheckStatus{Are clothes marked DONE?}
    CheckStatus -- No --> TellCustomer[Tell customer: 'Garment still being stitched/ironed']
    CheckStatus -- Yes --> CheckMoney{Is remaining balance ₹0?}
    
    CheckMoney -- Yes (Fully Paid) --> DeliverCloth[Click 'Deliver Garment' -> Marked DELIVERED]
    
    CheckMoney -- No (Money Pending) --> BlockScreen[SYSTEM BLOCKS DELIVERY<br/>Shows red warning with balance amount]
    BlockScreen --> WhatNext{What does customer do?}
    
    WhatNext -- Customer pays balance now --> TakePayment[Click 'Add Payment' (Cash or UPI)]
    TakePayment --> DeliverCloth
    
    WhatNext -- Customer asks to pay later --> CheckWhoIsLogged{Who is using computer?}
    CheckWhoIsLogged -- Tailor --> TailorBlocked[Tailor cannot deliver without money.<br/>Must call the shop owner.]
    CheckWhoIsLogged -- Owner --> OwnerReason[Owner types reason: e.g. 'Trusted neighbor, pays tonight']
    OwnerReason --> SaveAudit[System saves audit log with owner name & reason]
    SaveAudit --> DeliverCloth
```

---

## 5. Important Rules That Keep the System Safe

1. **Balance Is Always Calculated Fresh**:
   The database never stores a static "balance" number. Instead, every time a screen opens, the system calculates:
   $$\text{Balance} = \text{Total Price of Items} - \text{Total Payments Made}$$
   This guarantees that balance numbers are always 100% accurate and can never get out of sync.

2. **Old Measurements Are Never Overwritten**:
   When an old customer comes in and their measurements changed slightly, the system creates a brand-new measurement snapshot with today's date. The old record from 6 months ago stays untouched, so old receipts and job cards are never changed.

3. **Accurate Money Calculations**:
   All money amounts use exact decimal math (`BigDecimal`). We never use floating-point numbers (`double` or `float`), ensuring zero rounding errors even on ₹0.50.
