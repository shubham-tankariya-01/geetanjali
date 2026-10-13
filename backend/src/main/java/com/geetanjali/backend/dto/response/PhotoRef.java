package com.geetanjali.backend.dto.response;

// PhotoRef: A lightweight immutable record representing an uploaded photo reference.
// Returned by PhotoStorageService and serialized to JSON by PhotoApiController for frontend usage.
// In Express, this corresponds to `{ url: '/uploads/abc.jpg', publicId: 'abc.jpg' }`.
//
// In Java 21: A `record` is a compact, transparent carrier for immutable data.
// The compiler automatically generates:
// - private final fields for url and publicId
// - canonical constructor
// - accessor methods (url() and publicId())
// - equals(), hashCode(), and toString()
public record PhotoRef(
        // Web-accessible URL path to view the photo in <img> tags (e.g. "/uploads/123e4567-e89b-12d3-a456-426614174000.jpg")
        String url,

        // Unique storage identifier or filename used to reference or delete the photo later (e.g. "123e4567-e89b-12d3-a456-426614174000.jpg")
        String publicId
) {
}
