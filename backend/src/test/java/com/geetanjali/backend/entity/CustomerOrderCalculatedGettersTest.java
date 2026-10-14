package com.geetanjali.backend.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// CustomerOrderCalculatedGettersTest: Unit tests verifying calculated helpers on CustomerOrder.
// Tests total order calculation, item count, earliest delivery date resolution, and balance due calculation.
class CustomerOrderCalculatedGettersTest {

    @Test
    @DisplayName("Empty order returns zero total, zero items, and null nearest delivery date")
    void testEmptyOrder_returnsDefaults() {
        CustomerOrder order = new CustomerOrder();

        assertEquals(BigDecimal.ZERO, order.getTotalAmount());
        assertEquals(BigDecimal.ZERO, order.getTotal());
        assertEquals(0, order.getItemCount());
        assertNull(order.getNearestDeliveryDate());
        assertEquals(BigDecimal.ZERO, order.getTotalPaid());
        assertEquals(BigDecimal.ZERO, order.getPaid());
        assertEquals(BigDecimal.ZERO, order.getBalanceDue());
        assertEquals(BigDecimal.ZERO, order.getBalance());
        org.junit.jupiter.api.Assertions.assertTrue(order.isPaid());
    }

    @Test
    @DisplayName("Order with multiple items correctly sums total and identifies nearest delivery date")
    void testOrderWithItems_calculatesTotalsAndNearestDelivery() {
        CustomerOrder order = new CustomerOrder();
        Customer customer = new Customer("Ramesh Patel", "9876543210");
        order.setCustomer(customer);

        Garment shirt = new Garment("Shirt", 1, true);
        Garment pant = new Garment("Pant", 2, true);

        OrderItem item1 = new OrderItem(order, shirt, null, "Double pocket", new BigDecimal("450.00"), LocalDate.of(2026, 10, 25));
        OrderItem item2 = new OrderItem(order, pant, null, "Narrow bottom", new BigDecimal("550.00"), LocalDate.of(2026, 10, 20));
        OrderItem item3 = new OrderItem(order, shirt, null, "Casual shirt", new BigDecimal("400.00"), LocalDate.of(2026, 10, 28));

        order.setItems(List.of(item1, item2, item3));

        // 450 + 550 + 400 = 1400.00
        assertEquals(new BigDecimal("1400.00"), order.getTotalAmount());
        assertEquals(new BigDecimal("1400.00"), order.getTotal());
        assertEquals(new BigDecimal("1400.00"), order.getBalance());
        org.junit.jupiter.api.Assertions.assertFalse(order.isPaid());
        assertEquals(3, order.getItemCount());
        // Nearest date is 2026-10-20
        assertEquals(LocalDate.of(2026, 10, 20), order.getNearestDeliveryDate());
    }

    @Test
    @DisplayName("Order with advance payment correctly calculates total paid and balance due")
    void testOrderFinancials_calculatesBalanceDue() {
        CustomerOrder order = new CustomerOrder();

        Garment kurta = new Garment("Kurta", 3, true);
        OrderItem item = new OrderItem(order, kurta, null, "Festive wear", new BigDecimal("800.00"), LocalDate.of(2026, 11, 5));
        order.setItems(List.of(item));

        Payment advancePayment = new Payment(order, new BigDecimal("300.00"), "UPI", LocalDate.of(2026, 10, 15));
        order.setPayments(List.of(advancePayment));

        assertEquals(new BigDecimal("800.00"), order.getTotalAmount());
        assertEquals(new BigDecimal("800.00"), order.getTotal());
        assertEquals(new BigDecimal("300.00"), order.getTotalPaid());
        assertEquals(new BigDecimal("300.00"), order.getPaid());
        // 800 - 300 = 500
        assertEquals(new BigDecimal("500.00"), order.getBalanceDue());
        assertEquals(new BigDecimal("500.00"), order.getBalance());
        org.junit.jupiter.api.Assertions.assertFalse(order.isPaid());
    }

    @Test
    @DisplayName("Order with overpayment or full payment clamps balance due to zero")
    void testOrderFinancials_clampsBalanceDueToZero() {
        CustomerOrder order = new CustomerOrder();

        Garment blouse = new Garment("Blouse", 4, true);
        OrderItem item = new OrderItem(order, blouse, null, "Embroidery", new BigDecimal("500.00"), LocalDate.of(2026, 11, 1));
        order.setItems(List.of(item));

        Payment payment1 = new Payment(order, new BigDecimal("300.00"), "CASH", LocalDate.of(2026, 10, 15));
        Payment payment2 = new Payment(order, new BigDecimal("300.00"), "UPI", LocalDate.of(2026, 10, 16));
        order.setPayments(List.of(payment1, payment2)); // Total paid 600

        assertEquals(new BigDecimal("500.00"), order.getTotalAmount());
        assertEquals(new BigDecimal("500.00"), order.getTotal());
        assertEquals(new BigDecimal("600.00"), order.getTotalPaid());
        assertEquals(new BigDecimal("600.00"), order.getPaid());
        // Balance due should not be negative; clamped to zero
        assertEquals(BigDecimal.ZERO, order.getBalanceDue());
        // Pure arithmetic balance is 500 - 600 = -100
        assertEquals(new BigDecimal("-100.00"), order.getBalance());
        org.junit.jupiter.api.Assertions.assertTrue(order.isPaid());
    }
}
