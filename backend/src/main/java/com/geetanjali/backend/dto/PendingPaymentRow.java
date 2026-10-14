package com.geetanjali.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// PendingPaymentRow: Lightweight immutable record representing a single order row on the Pending Payments ledger.
//
// What it is: A read-only data transfer object (DTO) that groups together an unpaid order's identification,
// customer contact information, calculated remaining balance, promised delivery date, and human-friendly due text.
//
// Why a Java record? Records are concise, immutable data carriers introduced in modern Java (Java 16+).
// They automatically generate constructors, getters, equals(), hashCode(), and toString() without boilerplate code.
public record PendingPaymentRow(
        // Primary key ID of the customer order (used to build URLs like /orders/{id}#payments-section).
        Long orderId,

        // Sequential order number integer from the database sequence (e.g. 1001).
        Long orderNo,

        // Formatted order sequence string for display (e.g. "TS-2026-1001").
        String orderNumber,

        // Full name of the customer who placed the order.
        String customerName,

        // Primary 10-digit mobile number of the customer.
        String customerPhone,

        // Primary key ID of the customer (useful for linking to customer profile).
        Long customerId,

        // Calculated remaining balance (Order Total - Sum of all payments received so far).
        BigDecimal balance,

        // Earliest promised delivery date among all garments in this order (used for chronological sorting).
        LocalDate earliestDeliveryDate,

        // Formatted status text: "delivered N days ago", or "due in N days", or "overdue by N days".
        String dueText
) {
}
