package com.geetanjali.backend.entity;

import com.geetanjali.backend.enums.ItemStatus;
import com.geetanjali.backend.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// OrderItemTest: Unit tests for OrderItem lifecycle status transitions.
// Verifies that markDone only succeeds when item is PENDING and records doneAt timestamp.
class OrderItemTest {

    // Helper method to create a basic OrderItem in default PENDING status.
    private OrderItem createTestItem() {
        Garment garment = new Garment("Kurta", 1, true);
        return new OrderItem(
                null,
                garment,
                null,
                "Mandarin collar",
                new BigDecimal("1200"),
                LocalDate.of(2026, 10, 25)
        );
    }

    // Test: New item defaults to PENDING status and has null doneAt / deliveredAt.
    @Test
    @DisplayName("New OrderItem initializes with PENDING status")
    void testNewItem_defaultsToPending() {
        OrderItem item = createTestItem();

        assertEquals(ItemStatus.PENDING, item.getStatus());
        assertTrue(item.isPending());
        assertFalse(item.isDone());
        assertFalse(item.isDelivered());
    }

    // Test: Calling markDone on PENDING item transitions status to DONE and records timestamp.
    @Test
    @DisplayName("markDone transitions PENDING item to DONE and records doneAt timestamp")
    void testMarkDone_whenPending_transitionsToDone() {
        OrderItem item = createTestItem();
        Instant completedTime = Instant.parse("2026-10-14T11:30:00Z");

        item.markDone(completedTime);

        assertEquals(ItemStatus.DONE, item.getStatus());
        assertEquals(completedTime, item.getDoneAt());
        assertFalse(item.isPending());
        assertTrue(item.isDone());
        assertFalse(item.isDelivered());
    }

    // Test: Calling markDone on an item already marked DONE throws BusinessRuleException.
    @Test
    @DisplayName("markDone throws BusinessRuleException when item is already DONE")
    void testMarkDone_whenAlreadyDone_throwsBusinessRuleException() {
        OrderItem item = createTestItem();
        item.markDone(Instant.now());

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> item.markDone(Instant.now())
        );

        assertTrue(exception.getMessage().contains("expected PENDING"));
    }

    // Test: Calling markDone on an item already DELIVERED throws BusinessRuleException.
    @Test
    @DisplayName("markDone throws BusinessRuleException when item is DELIVERED")
    void testMarkDone_whenDelivered_throwsBusinessRuleException() {
        OrderItem item = createTestItem();
        item.setStatus(ItemStatus.DELIVERED);

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> item.markDone(Instant.now())
        );

        assertTrue(exception.getMessage().contains("expected PENDING"));
    }
}
