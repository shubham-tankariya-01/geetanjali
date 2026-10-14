package com.geetanjali.backend.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// OverrideForm: Form-backing DTO for owner delivery override requests.
// Used when an owner approves garment handover even though the order has an unpaid balance.
// Server-side validation guarantees the reason is non-empty and under 500 characters.
public class OverrideForm {

    // Shop owner's mandatory explanation for overriding the balance block.
    // Must not be blank (empty or spaces only).
    @NotBlank(message = "Reason is required for delivery override")
    // Maximum length 500 characters as defined in requirements.
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;

    // Default zero-argument constructor required by Spring MVC form binding.
    public OverrideForm() {
    }

    // Convenience constructor for tests and controllers.
    // Input: reason (String) - justification for overriding balance block.
    public OverrideForm(String reason) {
        this.reason = reason;
    }

    // Getter for reason.
    // Output: String reason.
    public String getReason() {
        return reason;
    }

    // Setter for reason.
    // Input: String reason.
    public void setReason(String reason) {
        this.reason = reason;
    }
}
