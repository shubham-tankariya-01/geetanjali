package com.geetanjali.backend.util;

import com.geetanjali.backend.entity.CustomerOrder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;

// OrderNumberFormatter: Utility component to format order sequence numbers and money consistently.
// In Thymeleaf templates, this is called directly as ${@orderNumberFormatter.format(order)} or ${@orderNumberFormatter.formatMoney(amount)}.
// Why a Spring @Component? It allows direct expression evaluation in templates without cluttering controller models.
// @Component = Tells Spring to manage this class as a singleton bean in the application context.
@Component
public class OrderNumberFormatter {

    // Clock instance configured to Asia/Kolkata timezone.
    // Why inject Clock? Relying on system default clock leads to timezone errors across different servers.
    // Injecting Clock ensures reliable timezone conversion and enables deterministic unit tests.
    private final Clock clock;

    // Constructor injection for Clock dependency.
    // Input: Clock clock (injected from AppConfig).
    public OrderNumberFormatter(Clock clock) {
        this.clock = clock;
    }

    // Formats an order's sequence number as TS-<year>-<4-digit order_no>, e.g. TS-2026-0014.
    // Input: CustomerOrder order.
    // Output: Formatted String, e.g. "TS-2026-0014", or empty String if order or orderNo is null.
    public String format(CustomerOrder order) {
        if (order == null || order.getOrderNo() == null) {
            return "";
        }
        // Why convert Instant to Asia/Kolkata? Orders store created_at in UTC.
        // Converting to the shop's local timezone ensures the correct calendar year is rendered even near midnight on New Year's Eve.
        int year;
        if (order.getCreatedAt() != null) {
            year = order.getCreatedAt().atZone(clock.getZone()).getYear();
        } else {
            year = LocalDate.now(clock).getYear();
        }
        return String.format(Locale.ROOT, "TS-%d-%04d", year, order.getOrderNo());
    }

    // Formats a money amount as "Rs. 1,500" with commas and no decimal places when whole.
    // Example: 1500 or 1500.00 -> "Rs. 1,500". 1500.50 -> "Rs. 1,500.50".
    // Input: BigDecimal amount.
    // Output: Formatted String, e.g. "Rs. 1,500" or "Rs. 0" if null.
    public String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "Rs. 0";
        }
        // Strip trailing zeros to check whether the amount has non-zero decimal paise
        BigDecimal stripped = amount.stripTrailingZeros();
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"));
        if (stripped.scale() <= 0) {
            formatter.setMinimumFractionDigits(0);
            formatter.setMaximumFractionDigits(0);
            return "Rs. " + formatter.format(amount);
        } else {
            formatter.setMinimumFractionDigits(2);
            formatter.setMaximumFractionDigits(2);
            return "Rs. " + formatter.format(amount);
        }
    }
}
