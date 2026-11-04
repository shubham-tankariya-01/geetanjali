package com.geetanjali.backend.dto.form;

import com.geetanjali.backend.enums.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

// PaymentForm: Form-backing Data Transfer Object (DTO) for logging payments against an existing order.
// In Express, this corresponds to `req.body` containing amount, mode, paidOn, and returnTo.
// Why a separate DTO? Separates user HTTP input validation from database entity lifecycle (separation of concerns).
public class PaymentForm {

    // Amount of money received from the customer.
    // @NotNull = Field must not be null.
    @NotNull(message = "Payment amount is required")
    // @DecimalMin = Must be greater than zero.
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    private BigDecimal amount;

    // Payment method: CASH or UPI.
    // @NotNull = Mode cannot be null.
    @NotNull(message = "Payment mode is required")
    private PaymentMode mode = PaymentMode.CASH;

    // Date when payment was received.
    // @NotNull = Date cannot be null.
    @NotNull(message = "Payment date is required")
    private LocalDate paidOn;

    // Optional return URL to redirect back to (e.g. from Order Detail or Pending Payments list).
    // Sanitized in controller to prevent open-redirect vulnerabilities.
    private String returnTo;

    // Zero-argument default constructor required by Spring MVC form binding.
    public PaymentForm() {
    }

    // Convenience constructor for manual or test instantiation.
    // Inputs: amount (BigDecimal), mode (PaymentMode), paidOn (LocalDate), returnTo (String).
    public PaymentForm(BigDecimal amount, PaymentMode mode, LocalDate paidOn, String returnTo) {
        this.amount = amount;
        this.mode = (mode != null) ? mode : PaymentMode.CASH;
        this.paidOn = paidOn;
        this.returnTo = returnTo;
    }

    // Getter for amount.
    // Output: BigDecimal amount.
    public BigDecimal getAmount() {
        return amount;
    }

    // Setter for amount.
    // Input: BigDecimal amount.
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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

    // Getter for paidOn.
    // Output: LocalDate paidOn.
    public LocalDate getPaidOn() {
        return paidOn;
    }

    // Setter for paidOn.
    // Input: LocalDate paidOn.
    public void setPaidOn(LocalDate paidOn) {
        this.paidOn = paidOn;
    }

    // Getter for returnTo.
    // Output: String returnTo.
    public String getReturnTo() {
        return returnTo;
    }

    // Setter for returnTo.
    // Input: String returnTo.
    public void setReturnTo(String returnTo) {
        this.returnTo = returnTo;
    }
}
