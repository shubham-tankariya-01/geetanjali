# Geetanjali Tailoring Studio: Studio AI Assistant & Dual-Engine Tool Integration

> **Created By**: Shubham Tankariya & Sumit Tailor  
> **Key Java Classes**: `AssistantService.java`, `ToolRegistryService.java`  

---

## 1. Why an In-App AI Assistant for a Tailor Shop?

Tailors working on the cutting bench often have fabric shears in hand, chalk on their fingers, or a cloth bundle being pinned. Having to stop, put down the scissors, navigate through multiple menu screens, and click buttons interrupts their work.

To make things effortless, Geetanjali has a built-in **Studio AI Assistant**:

```mermaid
graph TD
    Button["Floating Chat Button (Bottom Right)"] --> Drawer["Side Drawer Opens Smoothly"]
    Drawer --> Mode{"Size Options"}
    Mode --> Normal["Normal Drawer (420px)"]
    Mode --> Wide["Wide Mode (720px) - Great for big measurement tables"]
    
    Drawer --> Question["Tailor asks question in English, Hindi, or Hinglish"]
    Question --> Engine["Assistant Service"]
    
    Engine --> Provider{"Dual AI Engine Pipeline"}
    Provider -->|Primary (Super Fast)| Groq["Groq Cloud API (Llama / DeepSeek)"]
    Provider -.->|Automatic Fallback| Gemini["Google Gemini (gemini-2.5-flash)"]
    
    Groq --> RealData{"Does AI need shop data?"}
    Gemini --> RealData
    
    RealData -- Yes --> Tools["Tool Registry executes real database query"]
    Tools --> Answer["AI gives accurate answer with clean tables and links!"]
```

---

## 2. How the Assistant Looks & Feels on Screen

1. **Floating Chat Icon**: A clean, rounded icon in the bottom-right corner of every page.
2. **Slide-Out Drawer**: Clicking it opens a side drawer without reloading or leaving your current screen.
3. **Wide Screen Mode**: A maximize button widens the drawer from 420px to 720px, making it easy to read wide measurement tables.
4. **Rich Nested Tables & Clickable Links**: 
   - Order numbers like `ORD-202611-0004` are rendered as clickable links that jump straight to the order page.
   - Unpaid balances are highlighted cleanly.
   - Measurements are displayed as bold tables inside tables.

---

## 3. Dual AI Engine: Why We Use Both Groq & Google Gemini

Tailor shops cannot afford to have their AI assistant stop working just because one company’s server had a glitch or rate limit. 

Geetanjali uses an **automatic two-engine backup system**:

1. **Primary Engine — Groq Cloud**:
   - Groq is extremely fast (responds in under 0.5 seconds).
   - Handles everyday questions, customer lookups, and calendar dates almost instantly.
2. **Backup Engine — Google Gemini (`gemini-2.5-flash`)**:
   - If Groq ever experiences network slowness or a rate limit, the system **automatically switches to Gemini** in the background.
   - The tailor doesn't have to do anything—the answer simply appears!

---

## 4. The 5 Real Database Tools (Zero Guesswork)

The assistant does not make up or hallucinate fake numbers. When asked a question, it calls **read-only database tools**:

### 1. `lookupCustomer`
- **What it does**: Searches customers by 10-digit mobile number or name.
- **Example Question**: *"Is Ketan Tankariya registered and how many orders has he made?"*

### 2. `getOrderDetails`
- **What it does**: Pulls up the full order status, list of garments, total bill, advance paid, and current balance.
- **Example Question**: *"What is the balance pending on order ORD-202611-0004?"*

### 3. `checkDeliveryCapacity`
- **What it does**: Checks how many garments are already scheduled for any date, and whether the workshop has standard capacity, requires overbooking confirmation, or has reached its maximum daily cap.
- **Example Question**: *"Can we take 2 more shirts for delivery on Friday?"*

### 4. `getCalendarSchedule`
- **What it does**: Shows all garments due on a specific day or across the whole month.
- **Example Question**: *"What clothes are due for delivery today?"*

### 5. `getGarmentMeasurements`
- **What it does**: Pulls up the exact saved measurement table for a customer's shirt, pant, or kurta.
- **Example Question**: *"What was Ketan's chest and waist size on his last shirt?"*

---

## 5. Built-in Safety & Tailor Language Understanding

1. **Strictly Read-Only**: The AI can only read information. It **cannot** delete records, change prices, or bypass payment blocks. All changes must be done by humans through normal screens.
2. **Fast & Focused**: The conversation keeps only the last 10 messages in memory so it never slows down or runs out of memory.
3. **Understands Indian Tailor Vernacular**: The assistant easily understands common tailoring words like *Kamar* (waist), *Chhati* (chest), *Lambai* (length), *Baki* (balance), *Advance*, and *Mori* (bottom hem).
