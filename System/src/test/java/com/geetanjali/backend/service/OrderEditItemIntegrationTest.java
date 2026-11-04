package com.geetanjali.backend.service;

import com.geetanjali.backend.dto.draft.OrderDraft;
import com.geetanjali.backend.dto.draft.OrderDraftItem;
import com.geetanjali.backend.dto.form.AdvanceForm;
import com.geetanjali.backend.dto.form.ItemForm;
import com.geetanjali.backend.entity.AppUser;
import com.geetanjali.backend.entity.Customer;
import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.entity.Measurement;
import com.geetanjali.backend.entity.OrderItem;
import com.geetanjali.backend.enums.PaymentMode;
import com.geetanjali.backend.repository.AppUserRepository;
import com.geetanjali.backend.repository.CustomerOrderRepository;
import com.geetanjali.backend.repository.CustomerRepository;
import com.geetanjali.backend.repository.GarmentRepository;
import com.geetanjali.backend.repository.MeasurementRepository;
import com.geetanjali.backend.repository.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// OrderEditItemIntegrationTest: Verifies editing items and appending items to existing orders.
// Specifically validates that:
// 1. Changing a measurement value creates a new snapshot while preserving the old one.
// 2. Appending items to an existing order grows that order without creating a duplicate CustomerOrder.
@SpringBootTest
class OrderEditItemIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private MeasurementRepository measurementRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private GarmentRepository garmentRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private Clock clock;

    private AppUser testUser;
    private Garment shirt;
    private Garment pant;
    private Customer customer;

    @BeforeEach
    void setUp() {
        testUser = appUserRepository.findAll().stream().findFirst().orElseThrow();
        shirt = garmentRepository.findAll().stream().filter(g -> g.getName().equalsIgnoreCase("Shirt")).findFirst().orElseThrow();
        pant = garmentRepository.findAll().stream().filter(g -> g.getName().equalsIgnoreCase("Pant")).findFirst().orElseThrow();
        customer = customerRepository.findByMobile("9988776655")
                .orElseGet(() -> customerRepository.save(new Customer("Test Edit Customer", "9988776655")));
    }

    @Test
    @DisplayName("Editing item measurements creates a new snapshot and preserves the previous snapshot")
    void testEditItem_changingMeasurements_createsNewSnapshotAndPreservesOld() {
        // Step 1: Create an order with Shirt and initial chest size = 38.0
        OrderDraft draft = new OrderDraft();
        draft.setCustomer(customer.getId(), customer.getName(), customer.getMobile());

        Map<Long, BigDecimal> initialMeasurements = new LinkedHashMap<>();
        initialMeasurements.put(1L, new BigDecimal("38.0")); // Length
        initialMeasurements.put(2L, new BigDecimal("40.0")); // Chest

        LocalDate deliveryDate = LocalDate.now(clock).plusDays(7);
        OrderDraftItem draftItem = new OrderDraftItem(shirt, initialMeasurements, "Initial note", deliveryDate, new BigDecimal("500.00"), List.of());
        draft.addItem(draftItem);

        CustomerOrder initialOrder = orderService.placeOrder(draft, new AdvanceForm(BigDecimal.ZERO, PaymentMode.CASH), testUser);
        assertNotNull(initialOrder);

        List<OrderItem> items = orderItemRepository.findByOrderId(initialOrder.getId());
        assertEquals(1, items.size());
        OrderItem savedItem = items.get(0);
        assertNotNull(savedItem.getMeasurement());
        Long oldSnapshotId = savedItem.getMeasurement().getId();

        // Step 2: Edit the item - change Chest from 40.0 to 42.5
        ItemForm editForm = new ItemForm();
        editForm.setDeliveryDate(deliveryDate);
        editForm.setPrice(new BigDecimal("550.00"));
        editForm.setNotes("Updated note with mandarin collar");

        Map<Long, BigDecimal> updatedMeasurements = new LinkedHashMap<>();
        updatedMeasurements.put(1L, new BigDecimal("38.0"));
        updatedMeasurements.put(2L, new BigDecimal("42.5")); // Modified chest!
        editForm.setMeasurements(updatedMeasurements);

        OrderItem updatedItem = orderService.updateOrderItem(initialOrder.getId(), savedItem.getId(), editForm);

        // Verify updated item fields
        assertEquals(new BigDecimal("550.00"), updatedItem.getPrice());
        assertEquals("Updated note with mandarin collar", updatedItem.getNotes());
        assertNotNull(updatedItem.getMeasurement());

        Long newSnapshotId = updatedItem.getMeasurement().getId();
        // A new snapshot was created!
        assertNotEquals(oldSnapshotId, newSnapshotId, "Modifying measurements MUST create a fresh snapshot!");

        // The old snapshot STILL EXISTS in the database!
        assertTrue(measurementRepository.findById(oldSnapshotId).isPresent(), "Old measurement snapshot MUST be preserved!");
        assertTrue(measurementRepository.findById(newSnapshotId).isPresent(), "New measurement snapshot MUST exist in database!");
    }

    @Test
    @DisplayName("Appending an item to an existing order grows the order without generating a new CustomerOrder")
    void testAppendItem_growsExistingOrder() {
        // Step 1: Create initial order with 1 item
        OrderDraft initialDraft = new OrderDraft();
        initialDraft.setCustomer(customer.getId(), customer.getName(), customer.getMobile());
        OrderDraftItem item1 = new OrderDraftItem(shirt, Map.of(), "First shirt", LocalDate.now(clock).plusDays(5), new BigDecimal("400.00"), List.of());
        initialDraft.addItem(item1);

        CustomerOrder order = orderService.placeOrder(initialDraft, new AdvanceForm(BigDecimal.ZERO, PaymentMode.CASH), testUser);
        Long existingOrderId = order.getId();
        Long orderNo = order.getOrderNo();

        assertEquals(1, orderItemRepository.findByOrderId(existingOrderId).size());

        // Step 2: Start draft with existingOrderId to append a second item (Pant)
        OrderDraft appendDraft = new OrderDraft();
        appendDraft.setCustomer(customer.getId(), customer.getName(), customer.getMobile());
        appendDraft.setExistingOrderId(existingOrderId);

        OrderDraftItem item2 = new OrderDraftItem(pant, Map.of(), "Second pant", LocalDate.now(clock).plusDays(6), new BigDecimal("600.00"), List.of());
        appendDraft.addItem(item2);

        CustomerOrder appendedOrder = orderService.placeOrder(appendDraft, new AdvanceForm(BigDecimal.ZERO, PaymentMode.CASH), testUser);

        // Verify that the order ID and order number are the same!
        assertEquals(existingOrderId, appendedOrder.getId());
        assertEquals(orderNo, appendedOrder.getOrderNo());

        // Verify that the existing order now contains 2 items!
        List<OrderItem> allItems = orderItemRepository.findByOrderId(existingOrderId);
        assertEquals(2, allItems.size(), "Order should now contain 2 garment items!");
    }
}
