package com.geetanjali.backend.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// GarmentForm: Form-backing DTO for creating and editing garment categories (e.g., Kurta, Shirt, Blouse).
// In Express, this corresponds to the validated request payload in req.body.
public class GarmentForm {

    // Category display name.
    // @NotBlank = Rejects null, empty strings, and strings containing only whitespace characters.
    @NotBlank(message = "Garment category name is required")
    // @Size = Limits name length to 50 characters matching database column constraints.
    @Size(max = 50, message = "Garment category name must not exceed 50 characters")
    private String name;

    // Default zero-argument constructor: Required by Spring MVC for form data binding.
    public GarmentForm() {
    }

    // Convenience constructor for initializing form with a name.
    // Input: name (String).
    // Output: New GarmentForm instance.
    public GarmentForm(String name) {
        this.name = name;
    }

    // Getter for name.
    // Input: None.
    // Output: String.
    public String getName() {
        return name;
    }

    // Setter for name.
    // Input: String name.
    // Output: None (void).
    public void setName(String name) {
        this.name = name;
    }
}
