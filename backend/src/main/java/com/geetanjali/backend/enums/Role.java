package com.geetanjali.backend.enums;

// Role enum: Defines the user roles within the tailor shop application.
// In Express/FastAPI, this is like checking `req.user.role === 'OWNER'`.
// OWNER = The shop owner who has full administrative permissions (manage shop, add tailors).
// TAILOR = Staff member who creates orders, records measurements, and updates statuses.
public enum Role {
    OWNER,
    TAILOR
}
