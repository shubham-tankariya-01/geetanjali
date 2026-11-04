package com.geetanjali.backend.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

// MeasurementPrefill: Data transfer object holding the latest measurement snapshot values for prefilling a garment form.
// In Express, this is like `{ measurementId, values: { [fieldId]: 38.5 }, createdAt, date }` passed to an order wizard view.
// In Geetanjali, when a customer re-orders a Shirt, the form automatically pre-fills with their previous numbers so the tailor doesn't re-type them.
//
// Records in Java 16+ are concise, immutable data carriers with automatic constructor, getters, equals, hashCode, and toString.
public record MeasurementPrefill(
        // The ID of the measurement snapshot row in the measurement table.
        Long measurementId,

        // The map of measurement values, where the key is the GarmentField ID and the value is the recorded number.
        // For example: { 1: 30.0, 2: 38.0, 3: 34.0 } where 1 is Length, 2 is Chest, 3 is Waist.
        Map<Long, BigDecimal> values,

        // Timestamp when this snapshot was created (stored as UTC Instant in PostgreSQL).
        Instant createdAt,

        // Local date in Asia/Kolkata timezone (e.g. 2026-10-13) for display in templates.
        LocalDate date
) {
}
