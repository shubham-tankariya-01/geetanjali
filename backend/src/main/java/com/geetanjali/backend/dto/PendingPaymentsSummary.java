package com.geetanjali.backend.dto;

import java.math.BigDecimal;
import java.util.List;

// PendingPaymentsSummary: Aggregate DTO holding the total outstanding amount and all pending order rows.
//
// What it is: A compound container carrying the shop's total unsettled balance and the sorted list
// of individual pending order records.
//
// Why a record? Keeps data immutable and cleanly transfers multiple return values from the service layer to the controller.
public record PendingPaymentsSummary(
        // Total money yet to be collected across all unpaid orders in the tailor shop.
        BigDecimal totalOutstanding,

        // Chronological list of orders with a balance > 0, sorted by earliest delivery date (oldest first).
        List<PendingPaymentRow> rows
) {
}
