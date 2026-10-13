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
import com.geetanjali.backend.enums.PaymentMode;
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

        tailorUser = new AppUser(null, "9876543210", "hash", "Ketan Owner", null);
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
}
