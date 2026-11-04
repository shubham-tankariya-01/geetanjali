package com.geetanjali.backend.controller.api;

import java.time.LocalDate;

// CustomerLookupResponse: Immutable record representing the JSON payload returned by the customer mobile lookup API.
// In Express, this corresponds to the JSON object sent via `res.json({ found, name, orderCount, lastOrderDate })`.
// Java Records automatically provide private final fields, constructor, getters, equals, hashCode, and toString.
//
// Inputs / Attributes:
// - found (boolean): true if a customer with this mobile exists, false otherwise.
// - name (String): Customer's full name, or null if not found.
// - orderCount (long): Total number of previous orders placed by this customer.
// - lastOrderDate (LocalDate): Date of the customer's most recent order, or null if no previous orders.
public record CustomerLookupResponse(
        boolean found,
        String name,
        long orderCount,
        LocalDate lastOrderDate
) {

    // Factory method for creating a "not found" response.
    // Output: CustomerLookupResponse with found=false and empty fields.
    public static CustomerLookupResponse notFound() {
        return new CustomerLookupResponse(false, null, 0, null);
    }

    // Factory method for creating a "found" response.
    // Inputs: name (String), orderCount (long), lastOrderDate (LocalDate).
    // Output: CustomerLookupResponse with found=true and populated fields.
    public static CustomerLookupResponse found(String name, long orderCount, LocalDate lastOrderDate) {
        return new CustomerLookupResponse(true, name, orderCount, lastOrderDate);
    }
}
