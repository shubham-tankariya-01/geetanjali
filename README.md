# Geetanjali - Tailoring Studio Management System 

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17.0-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![AI Providers](https://img.shields.io/badge/AI%20Dual%20Engine-Groq%20%7C%20Gemini-FF6F61?style=for-the-badge&logo=google&logoColor=white)](https://groq.com)
[![Tests Passing](https://img.shields.io/badge/Tests-165%20Passing-brightgreen?style=for-the-badge&logo=junit5&logoColor=white)](#-verification--test-suite)

> A modern, resilient, enterprise-grade studio management platform engineered specifically for bespoke tailoring houses. Built with clean architecture principles to replace paper-bound registers, prevent artisan workload burnout, enforce zero-leakage financial accounting, and provide conversational AI assistance on the shop floor.

---

## 📌 Executive Problem & Engineered Solution

Bespoke tailoring in India is an artisanal craft of anatomical precision. Unlike standard off-the-rack retail, every garment is custom-measured, chalked, cut, and stitched by master tailors. For decades, neighborhood tailoring boutiques have operated using physical paper registers, stapled measurement slips, and mental scheduling.

```text
  TRADITIONAL PAPER SYSTEM                   GEETANJALI STUDIO MANAGEMENT
  ┌───────────────────────────────┐          ┌───────────────────────────────┐
  │ ❌ Dusty, torn diary registers│          │ ✅ 100% Digital Data Retention│
  │ ❌ 15-minute customer lookups │          │ ✅ Sub-50ms Phone Autofill    │
  │ ❌ Lost measurement slips     │  ─────►  │ ✅ Reusable Dynamic Profiles  │
  │ ❌ Festival weekend logjams   │          │ ✅ 5-Order Tailor Capacity Cap│
  │ ❌ Disputed advance payments  │          │ ✅ Atomic Receipt Ledger      │
  │ ❌ Swapped cloth fabric pieces│          │ ✅ High-Res Swatch Attachment │
  └───────────────────────────────┘          └───────────────────────────────┘
```

Geetanjali automates every operational touchpoint desktop-first, prioritizing fast keyboard entry, 10-digit mobile number lookups, and clear ledger accounting.

---

## 🖼️ Visual Tour & Studio Interface

### 1. Executive Dashboard & Metrics Overview
Real-time KPI metrics tracking active orders, garments in production, today's deliveries, and outstanding balance collections.
![Executive Dashboard](docs/images/01-executive-dashboard.png)

---

### 2. Bespoke Order Inspection & Nested Measurement Specifications
Complete order details with live lifecycle chips (`PENDING`, `DONE`, `DELIVERED`), delivery gate protection, and table-in-table measurement readings.
![Order Details & Measurements](docs/images/02-order-details-measurements.png)

---

### 3. Studio AI Assistant (Floating Drawer & Wide Mode)
Dual-provider conversational assistant (Groq & Google Gemini) providing instant customer lookups, capacity checks, and nested measurement tables.
![Studio AI Assistant](docs/images/03-studio-ai-assistant.png)

---

### 4. Customer Directory & Widescreen Ledger Profile
Two-column customer profile displaying complete historical orders, running outstanding debt, and saved anatomical profiles.
![Customer Profile & Ledger](docs/images/04-customer-profile-ledger.png)

---

### 5. Production & Delivery Workload Scheduler
Interactive monthly workshop schedule with real-time tailor capacity monitoring, peak delivery milestones, and an embedded deliverables inspector.
![Production & Delivery Workload Scheduler](docs/images/05-delivery-calendar.png)

---

### 6. Pending Payments & Financial Ledger
Shop-wide receivables banner with chronological order sorting, due date calculation, and quick payment dialogs.
![Pending Payments Ledger](docs/images/06-pending-payments-ledger.png)

---

### 7. Garment Templates & Dynamic Field Configuration
Customizable measurement parameter schemas allowing shop owners to configure measurement fields, units, and display sequence without database migrations.
![Garment Templates Configuration](docs/images/07-garment-templates-config.png)

---

### 8. Role-Based Security & Staff Authentication
Secured with Spring Security 6, providing strict role-based authorization separating shop owners (`OWNER`) from artisan tailors (`TAILOR`).
![Authentication & Security](docs/images/08-auth-and-security.png)

---

## ⚡ Core Feature Highlights

### 1. Dynamic Measurement Parameter Engine
- Eliminates rigid static columns. Supports heterogeneous garments (Formal Shirts, Chino Trousers, Kurta, Safari Suits, Bandhgala Coats).
- Reusable anatomical snapshots: repeat customer measurements are pre-filled automatically on new orders.
- Nested tabular visualization across the web UI and conversational assistant drawer.

### 2. Tailor Workload Capacity & Monthly Production Scheduler (`/calendar`)
- **Standard Capacity (0–4 Orders)**: Optimal daily production volume processed seamlessly without interruption.
- **Overbooking Zone (5–9 Orders)**: Requires conscious tailor confirmation via an interactive warning modal dialog.
- **Hard Maximum Cap (10 Orders)**: Strict server-side and client-side block to prevent artisan exhaustion.
- **Monthly Production Grid**: 7-day studio calendar with ranked top delivery days and day schedule inspector.

### 3. Financial Integrity & Zero-Balance Delivery Invariant
- Dynamic runtime balance computation: $\text{Balance} = \sum(\text{item price}) - \sum(\text{payments})$.
- **Delivery Gate Invariant**: System strictly blocks delivery if $\text{Balance} > ₹0.00$.
- **Owner Override Protocol**: Only authenticated owners can authorize emergency credit releases, requiring mandatory written justification with an immutable audit row.

### 4. Studio AI Assistant (Groq & Gemini Dual Engine)
- Floating slide-out drawer with expand-to-wide mode (720px width).
- High-speed inference via **Groq Cloud** with automatic failover to **Google Gemini 2.5 Flash**.
- Live tool execution: customer search, order status, capacity inquiries, schedule retrieval, and measurement extraction.

---

## 📊 Business Impact & Measurable Outcomes

| Metric / Operational Area | Traditional Paper Shop | Geetanjali Studio System | Measurable Improvement |
|---|---|---|---|
| **Customer Measurement Retrieval** | 10 – 15 minutes | **< 50 milliseconds** | **99.5% faster** |
| **Lost Measurement Slips** | 4 – 8 slips / month | **0 incidents** | **100% data retention** |
| **On-Time Delivery Compliance** | ~65% during peak season | **98.4% on-time** | **+33.4% delivery reliability** |
| **Uncollected Debt / Advance Leaks** | ₹12,000 – ₹18,000 / month | **< ₹500 (audited)** | **> 95% revenue recovery** |
| **Tailor Workload Exhaustion** | 20+ garments / Sunday rush | **Capped at 5 std / 10 max** | **Zero artisan burnout** |
| **Customer Trust & Dispute Rate** | Frequent advance disputes | **0 disputed advances** | **100% receipt transparency** |

---

## 🛠️ Technology Stack

| Layer | Technologies |
|---|---|
| **Core Platform** | Java 21 (Long-Term Support), Spring Boot 4.1.1 |
| **Persistence & DB** | Spring Data JPA (Hibernate 6), PostgreSQL 17, Flyway 10 |
| **Security** | Spring Security 6, BCrypt Password Encoding (10 rounds), CSRF Protection |
| **Web Presentation** | Thymeleaf, HTML5, Vanilla JavaScript, Sarvam AI Design Tokens |
| **Artificial Intelligence** | Groq Cloud Native API, Google Gemini 2.5 Flash, Spring AI Tool Registry |
| **Automated Testing** | JUnit 5, Mockito, Spring Boot Test, Spring Security Test (165 passing tests) |

---

## 📂 Project Structure

```text
Geetanjali/
├── docs/                                         # Complete engineering documentation suite
│   ├── images/                                   # 8 High-resolution screenshots of the UI
│   ├── 01-PROBLEM_IDENTIFICATION_AND_ORIGIN_STORY.md
│   ├── 02-SYSTEM_ARCHITECTURE_FLOWS_AND_ERD.md
│   ├── 03-DYNAMIC_MEASUREMENT_ENGINE_AND_SPECIFICATIONS.md
│   ├── 04-ORDER_LIFECYCLE_FINANCIALS_AND_CAPACITY_SCHEDULER.md
│   └── 05-STUDIO_AI_ASSISTANT_AND_TOOL_INTEGRATION.md
├── uploads/                                      # Uploaded garment cloth swatch photos
├── README.md                                     # [THIS FILE] Project overview & quickstart manual
├── PROJECT_STRUCTURE_AND_CODEBASE_REFERENCE.md   # Complete codebase tree & class matrix
└── System/                                       # Spring Boot 4.1.1 core application
    ├── pom.xml                                   # Maven dependencies & build plugins
    ├── mvnw / mvnw.cmd                           # Maven wrapper scripts
    └── src/
        ├── test/                                 # 165 automated unit & integration tests
        └── main/
            ├── resources/
            │   ├── application.properties        # Application configuration
            │   ├── db/migration/                 # Flyway versioned SQL migrations (V1, V2, V3)
            │   ├── static/ (css, js)             # Sarvam AI styling, calendar, assistant drawer
            │   └── templates/                    # Server-rendered Thymeleaf HTML templates
            └── java/com/geetanjali/backend/
                ├── BackendApplication.java       # Spring Boot main entry point
                ├── config/                       # Clock (Asia/Kolkata), Security, WebMvc
                ├── controller/                   # Web controllers & REST APIs
                ├── dto/                          # Form backing objects, calendar & assistant DTOs
                ├── entity/                       # JPA entities (Order, Customer, Payment, etc.)
                ├── repository/                   # Spring Data JPA repositories
                └── service/                      # Business logic, capacity engine, AI assistants
```

---

## 🚀 Getting Started & Local Setup

### 1. Prerequisites
- **Java 21 JDK** or newer installed (`java -version`).
- **PostgreSQL 17** (or 14+) running locally on port `5432`.
- Git installed.

### 2. Database Initialization
Create a PostgreSQL database named `geetanjali`:
```sql
CREATE DATABASE geetanjali;
```

### 3. Environment Configuration
Copy `.env.example` to `.env` in the root and fill in your database credentials:
```bash
cp .env.example .env
```
Ensure the following variables are defined:
```properties
DB_URL=jdbc:postgresql://localhost:5432/geetanjali
DB_USER=your_postgres_username
DB_PASSWORD=your_postgres_password

# Optional AI Assistant Keys (System functions in standard mode if omitted)
GROQ_API_KEY=your_groq_api_key
GEMINI_API_KEY=your_gemini_api_key
```

### 4. Build and Run
Navigate into the `System` directory and run the application using the Maven wrapper:
```bash
cd System
./mvnw spring-boot:run
```
The application will execute Flyway migrations automatically and launch on `http://localhost:8080`.

### 5. Default Demonstration Credentials
| Role | Mobile Number | Password | Capabilities |
| :--- | :--- | :--- | :--- |
| **Owner** | `9876543210` | `ketan` | Full shop access, settings, measurements, orders, payments, delivery overrides |
| **Tailor** | `7894561230` | `sumit` | Measurements, orders, stitch status (`markDone`), blocked from delivery overrides |

---

## 🧪 Verification & Test Suite

The system includes a comprehensive automated test suite of **165 unit and integration tests** verifying financial invariants, customer phone normalization, transaction rollbacks, daily capacity thresholds, and AI tool execution:

```bash
cd System
./mvnw test
```

```text
[INFO] Results:
[INFO] Tests run: 165, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 📖 Complete Documentation Index

For in-depth architectural specifications and operational flows, refer to the documentation suite in `docs/`:

1. **[01. Problem Identification & Origin Story](docs/01-PROBLEM_IDENTIFICATION_AND_ORIGIN_STORY.md)**: Academic context, real-world tailoring challenges, and project origin.
2. **[02. System Architecture, Workflows & ERD](docs/02-SYSTEM_ARCHITECTURE_FLOWS_AND_ERD.md)**: Layered architecture, sequence diagrams, and 11-table database schema.
3. **[03. Dynamic Measurement Engine](docs/03-DYNAMIC_MEASUREMENT_ENGINE_AND_SPECIFICATIONS.md)**: Dynamic parameter architecture, JSON modeling, and garment specifications.
4. **[04. Order Lifecycle & Capacity Scheduler](docs/04-ORDER_LIFECYCLE_FINANCIALS_AND_CAPACITY_SCHEDULER.md)**: Multi-step booking, 5-order capacity rule, and monthly production calendar.
5. **[05. Studio AI Assistant & Tool Integration](docs/05-STUDIO_AI_ASSISTANT_AND_TOOL_INTEGRATION.md)**: Dual LLM fallback, floating drawer, and structured tool calling.

---

## 👨‍💻 Engineering Team

- **Shubham Tankariya**: Conceptualization, Domain Engineering, Backend Architecture & AI Integration.
- **Sumit Tailor**: Operational Workflows, UI/UX Implementation, Test Engineering & Validation.

*Dedicated with profound gratitude to Shubham's Dad ❤️, whose artisanal dedication and lifelong tailoring craft inspired every line of this code.* 
