package com.geetanjali.backend.dto.form;

import com.geetanjali.backend.enums.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

// AdvanceForm: Form-backing DTO for Wizard Step 4: Advance payment and order confirmation.
// Captures the optional upfront deposit paid by the customer when booking custom garments.
//
// In Express, this corresponds to `req.body.advanceAmount` and `req.body.mode`.
public class AdvanceForm {

    // Advance deposit amount paid by the customer upfront. Defaults to ₹0.00.
    //
    // @NotNull = Advance amount input must be present.
    @NotNull(message = "Advance amount is required")
    // @DecimalMin = Advance amount cannot be negative.
    @DecimalMin(value = "0.00", message = "Advance amount cannot be negative")
    private BigDecimal advanceAmount = BigDecimal.ZERO;

    // Payment method used for the advance deposit (Cash or UPI). Defaults to CASH.
    //
    // @NotNull = Payment mode is required.
    @NotNull(message = "Payment mode is required")
    private PaymentMode mode = PaymentMode.CASH;

    // Default zero-argument constructor required by Spring form binding.
    public AdvanceForm() {
    }

    // Convenience constructor.
    // Inputs: advanceAmount (BigDecimal), mode (PaymentMode).
    public AdvanceForm(BigDecimal advanceAmount, PaymentMode mode) {
        this.advanceAmount = (advanceAmount != null) ? advanceAmount : BigDecimal.ZERO;
        this.mode = (mode != null) ? mode : PaymentMode.CASH;
    }

    // Getter for advanceAmount.
    // Output: BigDecimal.
    public BigDecimal getAdvanceAmount() {
        return advanceAmount;
    }

    // Setter for advanceAmount.
    // Input: BigDecimal advanceAmount.
    public void setAdvanceAmount(BigDecimal advanceAmount) {
        this.advanceAmount = (advanceAmount != null) ? advanceAmount : BigDecimal.ZERO;
    }

    // Getter for mode.
    // Output: PaymentMode.
    public PaymentMode getMode() {
        return mode;
    }

    // Setter for mode.
    // Input: PaymentMode mode.
    public void setMode(PaymentMode mode) {
        this.mode = mode;
    }
}
