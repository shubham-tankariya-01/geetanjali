package com.geetanjali.backend.exception;

import java.math.BigDecimal;

// BalancePendingException: Thrown when an attempt is made to deliver a garment while the customer order has an unpaid balance.
// Extends BusinessRuleException so it automatically triggers transaction rollbacks inside @Transactional services.
//
// WHY carry the balance in the exception?
// By carrying the pending balance as a typed field, the web controller can capture the exact amount
// and display a clear, helpful warning panel ("Balance Rs. 400 pending") instead of a generic error.
public class BalancePendingException extends BusinessRuleException {

    // The unpaid balance on the order at the moment delivery was attempted.
    private final BigDecimal balance;

    // The database ID of the garment item that was blocked from delivery.
    private final Long itemId;

    // Constructor recording the blocked balance and item ID.
    // Inputs:
    //   balance (BigDecimal) - remaining order balance.
    //   itemId (Long) - database ID of the garment item.
    public BalancePendingException(BigDecimal balance, Long itemId) {
        super("Cannot deliver item: Order has an unpaid balance of Rs. " + balance);
        this.balance = balance;
        this.itemId = itemId;
    }

    // Getter for the unpaid balance.
    // Output: BigDecimal balance.
    public BigDecimal getBalance() {
        return balance;
    }

    // Getter for the garment item ID.
    // Output: Long itemId.
    public Long getItemId() {
        return itemId;
    }
}
