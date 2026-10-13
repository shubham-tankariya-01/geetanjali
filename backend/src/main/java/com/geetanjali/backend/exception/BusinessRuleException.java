package com.geetanjali.backend.exception;

// BusinessRuleException: Unchecked runtime exception thrown when a domain business rule is violated.
// For example: "Cannot place order without garments", "Advance payment cannot exceed total order price",
// or "Cannot deliver unfinished garment without override authorization".
//
// WHY Unchecked (extends RuntimeException):
// In Spring Boot, methods annotated with @Transactional automatically ROLL BACK the database transaction
// when an unchecked exception (RuntimeException) is thrown. This guarantees data consistency without manual rollbacks.
//
// Express / FastAPI comparison: Similar to throwing a validation/business error inside a route or transaction
// that is caught by your global error-handling middleware.
public class BusinessRuleException extends RuntimeException {

    // Constructor with descriptive error message.
    // Input: message (String) - explains why the business rule was violated.
    // Output: New BusinessRuleException instance.
    public BusinessRuleException(String message) {
        super(message);
    }
}
