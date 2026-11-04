# Geetanjali Tailoring Studio: System Architecture, Workflows & Database Schema (ERD)

> **Document Classification**: Core Technical Architecture & Data Model  
> **Authors**: Shubham Tankariya & Sumit Tailor  
> **Tech Stack**: Java 21 (LTS) | Spring Boot 4.1.1 | Spring Data JPA | PostgreSQL 17 | Flyway 10 | Thymeleaf | Groq & Gemini AI  

---

## 1. High-Level Architectural Overview

Geetanjali is built as a high-performance, modular monolithic web application. Monolithic design was deliberately chosen over microservices to guarantee **immediate ACID transactional consistency**, eliminate network latency in shop desk operations, and provide seamless, zero-maintenance local deployment for neighborhood enterprises.

```mermaid
graph TD
    Client["Staff & Owner Browser (Desktop / Tablet)"]
    
    subgraph WebLayer ["Spring MVC & Security Web Layer"]
        Security["Spring Security 6 (BCrypt, Session & Role Enforcement)"]
        Controllers["Spring MVC Controllers (@Controller / @RestController)"]
        GlobalAdvice["Global Exception & Model Advice"]
    end
    
    subgraph BusinessLayer ["Business Logic & Domain Layer"]
        Services["Domain Services (@Service, @Transactional)"]
        AIOrchestrator["AI Assistant Orchestrator (Dual Groq / Gemini Engine)"]
        ToolEngine["Tool Registry Engine (Action Execution)"]
    end
    
    subgraph PersistenceLayer ["Persistence & Storage Layer"]
        SpringData["Spring Data JPA (Hibernate 6)"]
        FileStore["Local Disk Photo Storage Service"]
    end
    
    subgraph ExternalStorage ["Physical Storage & AI Gateways"]
        Postgres[("PostgreSQL 17 Database")]
        DiskStorage[("Disk Uploads Directory (/uploads)")]
        GroqAPI["Groq Cloud API (Llama 3 / DeepSeek)"]
        GeminiAPI["Google Gemini API (Gemini 2.5 Flash)"]
    end
    
    Client -->|HTTPS / Session Cookie| Security
    Security --> Controllers
    Controllers --> Services
    Controllers --> AIOrchestrator
    AIOrchestrator --> ToolEngine
    ToolEngine --> Services
    Services --> SpringData
    Services --> FileStore
    SpringData --> Postgres
    FileStore --> DiskStorage
    AIOrchestrator -->|Fallback Pipeline| GroqAPI
    AIOrchestrator -.->|Automatic Failover| GeminiAPI
```

---

## 2. Layered Separation of Concerns

The architecture strictly enforces clean separation across four distinct boundaries:

1. **Controller Layer (`com.geetanjali.backend.controller`)**:
   - Thin HTTP orchestration only.
   - Binds HTTP form inputs into validated DTOs (`@Valid`).
   - Delegates business execution to exactly one service method.
   - Returns a Thymeleaf template view name or a REST JSON response.
   - **Absolute Rule**: Controllers never access database repositories directly.

2. **Business Service Layer (`com.geetanjali.backend.service`)**:
   - Encapsulates all domain invariants, calculations, and transactional boundaries (`@Transactional`).
   - Computes dynamic balances and enforces capacity rules.
   - Coordinates multi-table writes with automatic rollback on runtime exceptions.

3. **Data Access Layer (`com.geetanjali.backend.repository`)**:
   - Spring Data JPA repository interfaces extending `JpaRepository`.
   - Utilizes derived query methods and parameterized JPQL queries with bound arguments to eliminate SQL injection.

4. **Domain Entity Layer (`com.geetanjali.backend.entity`)**:
   - Rich relational entities mapped using JPA/Hibernate annotations.
   - Encapsulates entity-level domain invariants (e.g., `OrderItem.markDone()`).

---

## 3. Comprehensive Database Schema & Entity Relationship Diagram (ERD)

The database schema is fully managed via Flyway migrations (`V1__core_schema.sql`, `V2__seed_defaults.sql`, `V3__stage2_delivery.sql`).

```mermaid
erDiagram
    shop ||--o{ app_user : "employs"
    app_user ||--o{ customer_order : "books"
    app_user ||--o{ delivery_override : "authorizes"
    customer ||--o{ customer_order : "places"
    customer ||--o{ measurement : "owns"
    garment ||--|{ garment_field : "specifies"
    garment ||--o{ order_item : "categorizes"
    garment ||--o{ measurement : "applies_to"
    measurement ||--|{ measurement_value : "contains"
    garment_field ||--o{ measurement_value : "quantifies"
    customer_order ||--|{ order_item : "includes"
    customer_order ||--o{ payment : "has_ledger"
    order_item ||--o{ item_photo : "attaches"
    order_item ||--o{ delivery_override : "audits"

    shop {
        bigint id PK
        varchar name "Shop display name"
        varchar address "Physical shop location"
        varchar phone "Official contact number"
        timestamptz created_at
    }

    app_user {
        bigint id PK
        varchar full_name "Staff / Owner name"
        varchar mobile UK "Normalized 10-digit login mobile"
        varchar password_hash "BCrypt (10 rounds)"
        varchar role "OWNER or TAILOR"
        timestamptz created_at
    }

    customer {
        bigint id PK
        varchar name "Customer full name"
        varchar mobile UK "Normalized 10-digit mobile"
        timestamptz created_at
    }

    customer_order {
        bigint id PK
        bigint order_no UK "Generated sequence number"
        bigint customer_id FK "References customer(id)"
        bigint created_by FK "References app_user(id)"
        timestamptz created_at "Creation timestamp"
    }

    order_item {
        bigint id PK
        bigint order_id FK "References customer_order(id)"
        bigint garment_id FK "References garment(id)"
        bigint measurement_id FK "Snapshot reference"
        varchar notes "Special tailor instructions"
        numeric price "Item stitching price (NUMERIC 10,2)"
        date delivery_date "Promised delivery date"
        varchar status "PENDING, DONE, DELIVERED"
        timestamptz done_at "When stitching was finished"
        timestamptz delivered_at "When picked up by customer"
    }

    delivery_override {
        bigint id PK
        bigint order_item_id FK "References order_item(id)"
        numeric balance_at_time "Outstanding balance when released"
        varchar reason "Mandatory business justification"
        bigint overridden_by FK "References app_user(id)"
        timestamptz created_at "Audit timestamp"
    }

    garment {
        bigint id PK
        varchar name "Shirt, Pant, Kurta, Safari, etc."
        integer sort_order "Display sequence"
        boolean active "Soft delete / visibility flag"
    }

    garment_field {
        bigint id PK
        bigint garment_id FK "References garment(id)"
        varchar name "Field key (e.g. chest, length)"
        varchar label "UI label (e.g. Chest / છાતી)"
        varchar unit "inches, cm"
        integer sort_order "Field sequence on form"
        boolean is_required "Mandatory entry flag"
    }

    measurement {
        bigint id PK
        bigint customer_id FK "References customer(id)"
        bigint garment_id FK "References garment(id)"
        timestamptz created_at "Immutable snapshot timestamp"
    }

    measurement_value {
        bigint measurement_id PK, FK "Composite PK"
        bigint field_id PK, FK "Composite PK"
        numeric value "Precision measurement (NUMERIC 6,2)"
    }

    payment {
        bigint id PK
        bigint order_id FK "References customer_order(id)"
        numeric amount "Payment amount (NUMERIC 10,2)"
        varchar mode "CASH or UPI"
        date payment_date "Date payment was received"
        timestamptz created_at "Audit timestamp"
    }

    item_photo {
        bigint id PK
        bigint order_item_id FK "References order_item(id)"
        varchar photo_url "Public URL path (/uploads/...)"
        varchar file_path "Absolute disk storage path"
        timestamptz created_at
    }
```

---

## 4. Core Operational Workflows

### 4.1. Order Booking & Multi-Step Wizard Flow

```mermaid
sequenceDiagram
    autonumber
    actor Staff as Tailor / Owner
    participant Wizard as OrderWizardController
    participant Service as OrderWizardService
    participant CapService as CalendarService
    participant DB as PostgreSQL Database

    Staff->>Wizard: Step 1: Enter Customer Mobile (10 digits)
    Wizard->>Service: Lookup or create customer profile
    Staff->>Wizard: Step 2: Select Garments (e.g., 2 Shirts + 1 Pant)
    Staff->>Wizard: Step 3: Enter Measurements & Delivery Date
    Wizard->>CapService: Check Daily Workload Capacity for Date
    alt Date Capacity >= 10 Orders
        CapService-->>Staff: HARD BLOCK: "Maximum 10 orders already scheduled"
    else Date Capacity 5-9 Orders
        CapService-->>Staff: OVERBOOKING MODAL: "Date already has N orders. Confirm add?"
        Staff->>Wizard: Confirm Overbooking
    else Date Capacity < 5 Orders
        CapService-->>Staff: Capacity OK (Green)
    end
    Staff->>Wizard: Step 4: Review Summary & Record Advance Payment
    Wizard->>Service: Commit Order in Atomic Transaction
    Service->>DB: INSERT customer_order, order_items, measurements, payments
    DB-->>Staff: Order Booked! Display Order ID (ORD-YYYYMM-XXXX)
```

---

### 4.2. Staged Garment Lifecycle State Machine

Each individual garment item progresses through a strict three-state lifecycle:

```mermaid
stateDiagram-v2
    [*] --> PENDING: Order Placed & Scheduled
    
    state PENDING {
        description: Fabric cut, chalked, and being stitched by tailor
    }
    
    PENDING --> DONE: Tailor Clicks 'Mark Stitched'
    
    state DONE {
        description: Garment pressed, inspected, and hanging on finished rack
    }
    
    DONE --> DELIVERED: Normal Handover (Balance = ₹0.00)
    
    state DELIVERED {
        description: Garment collected by customer, receipt closed
    }
    
    DONE --> DELIVERED: Owner Override (Balance > ₹0.00 + Reason + Audit)
    
    note right of DONE
        System BLOCKS delivery if balance > 0
        Unless OWNER provides documented override
    end note
```

---

### 4.3. Delivery Gate & Balance Verification Workflow

To eliminate unpaid garment leakage, the delivery gate enforces a strict invariant:

```mermaid
flowchart TD
    Start[Customer Arrives for Pickup] --> Lookup[Tailor Opens Order Detail]
    Lookup --> VerifyItems{Are all items DONE?}
    VerifyItems -- No --> AlertWait[Alert: Garment still in stitching / ironing]
    VerifyItems -- Yes --> CheckBalance{Is Order Balance = ₹0.00?}
    
    CheckBalance -- Yes (Fully Paid) --> DeliverOK[Click 'Deliver Garment' -> Status: DELIVERED]
    
    CheckBalance -- No (Outstanding Balance) --> BlockGate[SYSTEM BLOCKS DELIVERY<br/>Throws BalancePendingException]
    BlockGate --> ShowModal[Display Red Alert Panel with Outstanding Amount]
    
    ShowModal --> Choice{Customer Action}
    Choice -- Customer Pays Now --> AddPay[Click 'Add Payment' (Cash/UPI) -> Balance becomes ₹0.00]
    AddPay --> DeliverOK
    
    Choice -- Customer Requests Credit --> CheckRole{Current User Role?}
    CheckRole -- TAILOR --> Refuse[Tailor Forbidden from Overriding<br/>Prompt: 'Please consult shop owner']
    CheckRole -- OWNER --> OverrideForm[Owner Enters Mandatory Business Justification]
    OverrideForm --> AuditLog[Save Immutable DeliveryOverride Record<br/>Balance at time, Reason, Timestamp, Owner ID]
    AuditLog --> DeliverOK
```

---

## 5. Architectural Invariants & Data Integrity Guarantees

1. **Balance is Derived, Never Cached**:
   $$\text{Balance} = \sum(\text{order\_item.price}) - \sum(\text{payment.amount})$$
   Balance is calculated at runtime from underlying transactional rows. This completely eliminates data desynchronization bugs.
2. **Immutable Measurement Snapshots**:
   Whenever a customer’s measurements are edited or updated for a new order, the system **never modifies historical measurement records**. It generates a new timestamped `measurement` entity so past orders and printed job cards retain historical integrity.
3. **Indian Standard Time (IST) Determinism**:
   System operations do not call `LocalDate.now()`. All date logic injects a central `Clock` bean pinned to `Asia/Kolkata`, guaranteeing zero date discrepancies across midnight shifts or cloud server regional zones.
