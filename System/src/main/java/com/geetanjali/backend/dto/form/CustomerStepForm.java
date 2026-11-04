package com.geetanjali.backend.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// CustomerStepForm: Form-backing object (DTO) for Wizard Step 1: Customer identification.
// In Express, this corresponds to `req.body` sent from the customer step form.
// Uses Jakarta Validation annotations to enforce server-side validation rules.
public class CustomerStepForm {

    // Primary key ID if an existing customer was identified via phone lookup; null if new customer.
    private Long customerId;

    // 10-digit Indian mobile number. Validated on server with regex.
    //
    // @NotBlank = Rejects null, empty string, or whitespace-only input.
    @NotBlank(message = "Mobile number is required")
    // @Pattern = Enforces standard Indian 10-digit mobile starting with 6, 7, 8, or 9.
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
    private String mobile;

    // Full name of the customer (e.g. "Ramesh Patel").
    //
    // @NotBlank = Required for all customers.
    @NotBlank(message = "Customer name is required")
    // @Size = Limits length between 2 and 100 characters.
    @Size(min = 2, max = 100, message = "Customer name must be between 2 and 100 characters")
    private String name;

    // Optional existing order ID if adding garments to an already open order.
    private Long existingOrderId;

    // Default zero-argument constructor for Spring form binding.
    public CustomerStepForm() {
    }

    // Convenience constructor.
    // Inputs: customerId (Long), mobile (String), name (String).
    public CustomerStepForm(Long customerId, String mobile, String name) {
        this.customerId = customerId;
        this.mobile = mobile;
        this.name = name;
    }

    // Full constructor.
    // Inputs: customerId, mobile, name, existingOrderId.
    public CustomerStepForm(Long customerId, String mobile, String name, Long existingOrderId) {
        this.customerId = customerId;
        this.mobile = mobile;
        this.name = name;
        this.existingOrderId = existingOrderId;
    }

    // =========================================================================
    // Getters and Setters
    // =========================================================================

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getExistingOrderId() {
        return existingOrderId;
    }

    public void setExistingOrderId(Long existingOrderId) {
        this.existingOrderId = existingOrderId;
    }
}
