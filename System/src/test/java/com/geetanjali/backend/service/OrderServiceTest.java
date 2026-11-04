package com.geetanjali.backend.service;

import com.geetanjali.backend.dto.draft.OrderDraft;
import com.geetanjali.backend.dto.draft.OrderDraftItem;
import com.geetanjali.backend.dto.form.AdvanceForm;
import com.geetanjali.backend.entity.AppUser;
import com.geetanjali.backend.entity.Customer;
import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.entity.ItemPhoto;
import com.geetanjali.backend.entity.Measurement;
import com.geetanjali.backend.entity.OrderItem;
import com.geetanjali.backend.enums.ItemStatus;
import com.geetanjali.backend.enums.PaymentMode;
import com.geetanjali.backend.exception.BusinessRuleException;
import com.geetanjali.backend.repository.CustomerOrderRepository;
import com.geetanjali.backend.repository.CustomerRepository;
import com.geetanjali.backend.repository.ItemPhotoRepository;
import com.geetanjali.backend.repository.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// OrderServiceTest: Unit tests for order placement, item persistence, and customer resolution.
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ItemPhotoRepository itemPhotoRepository;

    @Mock
    private MeasurementService measurementService;

    @Mock
    private PaymentService paymentService;

    private Clock fixedClock;
    private OrderService orderService;
    private AppUser tailorUser;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(
                Instant.parse("2026-10-15T10:00:00Z"),
                ZoneId.of("Asia/Kolkata")
        );

        orderService = new OrderService(
                customerRepository,
                customerOrderRepository,
                orderItemRepository,
                itemPhotoRepository,
                measurementService,
                paymentService,
                fixedClock
        );

        tailorUser = new AppUser(null, "9876543210", "hash", "Ketan Tankariya", null);
    }

    @Test
    void testPlaceOrder_newCustomerWithItemAndAdvance_persistsAllEntities() {
        OrderDraft draft = new OrderDraft();
        draft.setCustomer(null, "Anil Kumar", "9812345678");

        Garment shirt = new Garment("Shirt", 1, true);
        OrderDraftItem item = new OrderDraftItem(
                shirt,
                Map.of(1L, new BigDecimal("40.0")),
                "Slim fit, double pocket",
                LocalDate.of(2026, 10, 22),
                new BigDecimal("500.00"),
                List.of("/uploads/swatch.jpg")
        );
        item.setPhotoPublicIds(List.of("swatch.jpg"));
        draft.addItem(item);

        AdvanceForm advanceForm = new AdvanceForm(new BigDecimal("200.00"), PaymentMode.UPI);

        Customer savedCustomer = new Customer("Anil Kumar", "9812345678");
        savedCustomer.setId(50L);
        when(customerRepository.findByMobile("9812345678")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

        when(customerOrderRepository.getNextOrderNo()).thenReturn(1001L);
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(inv -> {
            CustomerOrder o = inv.getArgument(0);
            o.setId(101L);
            return o;
        });

        Measurement dummyMeasurement = new Measurement(savedCustomer, shirt, fixedClock.instant());
        when(measurementService.saveSnapshot(any(Customer.class), any(Garment.class), any())).thenReturn(dummyMeasurement);

        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> {
            OrderItem oi = inv.getArgument(0);
            oi.setId(201L);
            return oi;
        });

        CustomerOrder placedOrder = orderService.placeOrder(draft, advanceForm, tailorUser);

        assertNotNull(placedOrder);
        assertEquals(1001L, placedOrder.getOrderNo());
        assertEquals(savedCustomer, placedOrder.getCustomer());
        assertEquals(tailorUser, placedOrder.getCreatedBy());

        // Verify OrderItem saved
        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(itemCaptor.capture());
        OrderItem savedItem = itemCaptor.getValue();
        assertEquals(shirt, savedItem.getGarment());
        assertEquals(dummyMeasurement, savedItem.getMeasurement());
        assertEquals("Slim fit, double pocket", savedItem.getNotes());
        assertEquals(new BigDecimal("500.00"), savedItem.getPrice());

        // Verify ItemPhoto saved
        ArgumentCaptor<ItemPhoto> photoCaptor = ArgumentCaptor.forClass(ItemPhoto.class);
        verify(itemPhotoRepository).save(photoCaptor.capture());
        ItemPhoto savedPhoto = photoCaptor.getValue();
        assertEquals("/uploads/swatch.jpg", savedPhoto.getUrl());
        assertEquals("swatch.jpg", savedPhoto.getPublicId());

        // Verify advance payment recorded
        verify(paymentService).recordAdvance(placedOrder, new BigDecimal("200.00"), PaymentMode.UPI);
    }

    @Test
    void testPlaceOrder_zeroItems_throwsIllegalArgumentException() {
        OrderDraft draft = new OrderDraft();
        draft.setCustomer(1L, "Ramesh Patel", "9876501234");
        AdvanceForm advanceForm = new AdvanceForm(BigDecimal.ZERO, PaymentMode.CASH);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                orderService.placeOrder(draft, advanceForm, tailorUser)
        );
        assertEquals("Cannot place order with zero garment items", ex.getMessage());
        verify(customerOrderRepository, never()).save(any());
    }

    @Test
    void testPlaceOrder_zeroAdvance_doesNotRecordPayment() {
        OrderDraft draft = new OrderDraft();
        draft.setCustomer(10L, "Existing Customer", "9123456780");
        Customer existingCustomer = new Customer("Existing Customer", "9123456780");
        existingCustomer.setId(10L);
        when(customerRepository.findById(10L)).thenReturn(Optional.of(existingCustomer));

        Garment pant = new Garment("Pant", 2, true);
        OrderDraftItem item = new OrderDraftItem(pant, Map.of(), "No lining", LocalDate.of(2026, 10, 25), new BigDecimal("400.00"), List.of());
        draft.addItem(item);

        when(customerOrderRepository.getNextOrderNo()).thenReturn(1002L);
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        AdvanceForm advanceForm = new AdvanceForm(BigDecimal.ZERO, PaymentMode.CASH);

        CustomerOrder placedOrder = orderService.placeOrder(draft, advanceForm, tailorUser);

        assertNotNull(placedOrder);
        verify(paymentService, never()).recordAdvance(any(), any(), any());
    }

    @Test
    void testMarkDone_pendingItem_success() {
        Long orderId = 1L;
        Long itemId = 5L;

        Customer customer = new Customer("Aarav", "9876543210");
        CustomerOrder order = new CustomerOrder(1001L, customer, tailorUser, fixedClock.instant());
        order.setId(orderId);

        Garment kurta = new Garment("Kurta", 1, true);
        OrderItem item = new OrderItem(order, kurta, null, "Fine stitch", new BigDecimal("800.00"), LocalDate.of(2026, 10, 20));
        item.setId(itemId);
        order.getItems().add(item);

        when(customerOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderItem updated = orderService.markDone(orderId, itemId);

        assertEquals(ItemStatus.DONE, updated.getStatus());
        assertEquals(fixedClock.instant(), updated.getDoneAt());
        verify(orderItemRepository).save(item);
    }

    @Test
    void testMarkDone_alreadyDone_throwsBusinessRuleException() {
        Long orderId = 1L;
        Long itemId = 5L;

        Customer customer = new Customer("Aarav", "9876543210");
        CustomerOrder order = new CustomerOrder(1001L, customer, tailorUser, fixedClock.instant());
        order.setId(orderId);

        Garment kurta = new Garment("Kurta", 1, true);
        OrderItem item = new OrderItem(order, kurta, null, "Fine stitch", new BigDecimal("800.00"), LocalDate.of(2026, 10, 20));
        item.setId(itemId);
        item.markDone(fixedClock.instant());
        order.getItems().add(item);

        when(customerOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        assertThrows(BusinessRuleException.class, () -> orderService.markDone(orderId, itemId));
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("getActiveOrdersSortedByDeliveryDate excludes orders where both payment and delivery are completed, and sorts remaining active orders by earliest delivery date")
    void testGetActiveOrdersSortedByDeliveryDate_filtersCompletedAndSortsAscending() {
        Customer customer = new Customer("Test Cust", "9876500000");

        // Order 1: Fully paid (1000 - 1000 = 0) and delivered -> COMPLETED -> MUST BE EXCLUDED
        CustomerOrder order1 = new CustomerOrder(1001L, customer, null, fixedClock.instant());
        order1.setId(1L);
        OrderItem item1 = new OrderItem(order1, null, null, "Item 1", new BigDecimal("1000.00"), LocalDate.of(2026, 10, 15));
        item1.markDelivered(fixedClock.instant());
        order1.getItems().add(item1);
        order1.getPayments().add(new com.geetanjali.backend.entity.Payment(order1, new BigDecimal("1000.00"), "CASH", LocalDate.of(2026, 10, 14)));

        // Order 2: Fully paid (800 - 800 = 0) but delivery PENDING (delivery date Oct 22) -> ACTIVE -> MUST BE INCLUDED
        CustomerOrder order2 = new CustomerOrder(1002L, customer, null, fixedClock.instant());
        order2.setId(2L);
        OrderItem item2 = new OrderItem(order2, null, null, "Item 2", new BigDecimal("800.00"), LocalDate.of(2026, 10, 22));
        order2.getItems().add(item2);
        order2.getPayments().add(new com.geetanjali.backend.entity.Payment(order2, new BigDecimal("800.00"), "UPI", LocalDate.of(2026, 10, 14)));

        // Order 3: Delivered with override, but payment PENDING (1200 - 500 = 700 balance, delivery date Oct 12) -> ACTIVE -> MUST BE INCLUDED
        CustomerOrder order3 = new CustomerOrder(1003L, customer, null, fixedClock.instant());
        order3.setId(3L);
        OrderItem item3 = new OrderItem(order3, null, null, "Item 3", new BigDecimal("1200.00"), LocalDate.of(2026, 10, 12));
        item3.markDelivered(fixedClock.instant());
        order3.getItems().add(item3);
        order3.getPayments().add(new com.geetanjali.backend.entity.Payment(order3, new BigDecimal("500.00"), "CASH", LocalDate.of(2026, 10, 14)));

        // Order 4: Neither paid nor delivered (earliest delivery date Oct 18) -> ACTIVE -> MUST BE INCLUDED
        CustomerOrder order4 = new CustomerOrder(1004L, customer, null, fixedClock.instant());
        order4.setId(4L);
        OrderItem item4 = new OrderItem(order4, null, null, "Item 4", new BigDecimal("500.00"), LocalDate.of(2026, 10, 18));
        order4.getItems().add(item4);

        when(customerOrderRepository.findAll()).thenReturn(List.of(order1, order2, order3, order4));

        List<CustomerOrder> result = orderService.getActiveOrdersSortedByDeliveryDate();

        // Order 1 is excluded because both payment and delivery are completed
        assertEquals(3, result.size());
        org.junit.jupiter.api.Assertions.assertFalse(result.contains(order1));

        // Sorting check: earliest delivery date ascending:
        // Order 3 (Oct 12) -> Order 4 (Oct 18) -> Order 2 (Oct 22)
        assertEquals(3L, result.get(0).getId());
        assertEquals(4L, result.get(1).getId());
        assertEquals(2L, result.get(2).getId());
    }
}
