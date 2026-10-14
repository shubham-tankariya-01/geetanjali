package com.geetanjali.backend.util;

import com.geetanjali.backend.entity.CustomerOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

// OrderNumberFormatterTest: Unit tests for OrderNumberFormatter.
// Tests TS-<year>-<4-digit order_no> generation using Asia/Kolkata timezone and Indian money formatting.
class OrderNumberFormatterTest {

    private OrderNumberFormatter formatter;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        // Fixed at 2026-10-14T06:00:00Z in Asia/Kolkata
        fixedClock = Clock.fixed(Instant.parse("2026-10-14T06:00:00Z"), ZoneId.of("Asia/Kolkata"));
        formatter = new OrderNumberFormatter(fixedClock);
    }

    @Test
    @DisplayName("Formats order number with leading zeros and year from Asia/Kolkata timezone")
    void testFormat_withValidOrder() {
        CustomerOrder order = new CustomerOrder();
        order.setOrderNo(14L);
        order.setCreatedAt(Instant.parse("2026-03-15T10:00:00Z"));

        String result = formatter.format(order);
        assertEquals("TS-2026-0014", result);
    }

    @Test
    @DisplayName("Formats order with single digit order number correctly")
    void testFormat_singleDigit() {
        CustomerOrder order = new CustomerOrder();
        order.setOrderNo(1L);
        order.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        String result = formatter.format(order);
        assertEquals("TS-2026-0001", result);
    }

    @Test
    @DisplayName("Returns empty string for null order or null orderNo")
    void testFormat_nullSafeguard() {
        assertEquals("", formatter.format(null));

        CustomerOrder order = new CustomerOrder();
        assertEquals("", formatter.format(order));
    }

    @Test
    @DisplayName("Formats whole currency amounts without decimals as Rs. 1,500")
    void testFormatMoney_wholeAmounts() {
        assertEquals("Rs. 1,500", formatter.formatMoney(new BigDecimal("1500")));
        assertEquals("Rs. 1,500", formatter.formatMoney(new BigDecimal("1500.00")));
        assertEquals("Rs. 400", formatter.formatMoney(new BigDecimal("400")));
        assertEquals("Rs. 0", formatter.formatMoney(BigDecimal.ZERO));
        assertEquals("Rs. 0", formatter.formatMoney(null));
    }

    @Test
    @DisplayName("Formats fractional currency amounts with 2 decimal places")
    void testFormatMoney_fractionalAmounts() {
        assertEquals("Rs. 450.50", formatter.formatMoney(new BigDecimal("450.50")));
        assertEquals("Rs. 1,500.75", formatter.formatMoney(new BigDecimal("1500.75")));
    }

    @Test
    @DisplayName("Returns today's date in Asia/Kolkata timezone from clock")
    void testGetToday() {
        assertEquals(LocalDate.of(2026, 10, 14), formatter.getToday());
    }
}
