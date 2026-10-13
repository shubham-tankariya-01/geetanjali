package com.geetanjali.backend.exception;

// ResourceNotFoundException: Unchecked runtime exception thrown when a requested database entity or resource is not found.
// For example: "Customer with ID 42 not found" or "Garment 'Shirt' not found".
//
// WHY Unchecked (extends RuntimeException):
// In Spring Boot, methods annotated with @Transactional automatically ROLL BACK the database transaction
// when an unchecked exception (RuntimeException) is thrown. Checked exceptions (extends Exception) do NOT
// trigger rollbacks by default.
//
// Express / FastAPI comparison: Similar to `throw new NotFoundError(...)` which your global error handler
// catches to send an HTTP 404 response.
public class ResourceNotFoundException extends RuntimeException {

    // Constructor with descriptive error message.
    // Input: message (String) - explains which resource was not found.
    // Output: New ResourceNotFoundException instance.
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
