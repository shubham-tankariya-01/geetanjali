package com.geetanjali.backend.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// FieldForm: Form-backing DTO for creating and updating measurement fields on garment templates.
// In Express, this corresponds to the validated request payload { label, unit } from req.body.
public class FieldForm {

    // Measurement field label (e.g., "Chest", "Waist", "Length", "Hip").
    // @NotBlank = Rejects null, empty strings, or blank whitespace.
    @NotBlank(message = "Field label is required")
    // @Size = Limits label length to 50 characters matching database column constraints.
    @Size(max = 50, message = "Field label must not exceed 50 characters")
    private String label;

    // Measurement unit (e.g., "inch", "cm").
    // Defaults to "inch" as standard tailoring unit.
    // @NotBlank = Ensures a measurement unit is explicitly present.
    @NotBlank(message = "Measurement unit is required")
    // @Size = Limits unit string length to 20 characters matching database column constraints.
    @Size(max = 20, message = "Unit must not exceed 20 characters")
    private String unit = "inch";

    // Default zero-argument constructor: Required by Spring MVC for form data binding.
    public FieldForm() {
    }

    // Convenience constructor.
    // Inputs: label (String), unit (String).
    // Output: New FieldForm instance.
    public FieldForm(String label, String unit) {
        this.label = label;
        this.unit = unit;
    }

    // Getter for label.
    // Input: None.
    // Output: String.
    public String getLabel() {
        return label;
    }

    // Setter for label.
    // Input: String label.
    // Output: None (void).
    public void setLabel(String label) {
        this.label = label;
    }

    // Getter for unit.
    // Input: None.
    // Output: String.
    public String getUnit() {
        return unit;
    }

    // Setter for unit.
    // Input: String unit.
    // Output: None (void).
    public void setUnit(String unit) {
        this.unit = unit;
    }
}
