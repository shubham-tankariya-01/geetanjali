package com.geetanjali.backend.service;

import com.geetanjali.backend.entity.AppUser;
import com.geetanjali.backend.entity.Customer;
import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.entity.DeliveryOverride;
import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.entity.OrderItem;
import com.geetanjali.backend.entity.Payment;
import com.geetanjali.backend.enums.ItemStatus;
import com.geetanjali.backend.enums.PaymentMode;
import com.geetanjali.backend.enums.Role;
import com.geetanjali.backend.exception.BalancePendingException;
import com.geetanjali.backend.exception.BusinessRuleException;
import com.geetanjali.backend.repository.CustomerOrderRepository;
import com.geetanjali.backend.repository.DeliveryOverrideRepository;
import com.geetanjali.backend.repository.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// DeliveryServiceTest: Unit tests verifying delivery rules, balance blocks, and owner overrides.
@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private DeliveryOverrideRepository deliveryOverrideRepository;

    private Clock fixedClock;
    private DeliveryService deliveryService;
    private AppUser ownerUser;
    private AppUser tailorUser;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-10-14T10:00:00Z"), ZoneId.of("Asia/Kolkata"));
        deliveryService = new DeliveryService(customerOrderRepository, orderItemRepository, deliveryOverrideRepository, fixedClock);

        ownerUser = new AppUser(null, "Owner Ketan", "9876543210", "hash", Role.OWNER);
        ownerUser.setId(1L);

        tailorUser = new AppUser(null, "Tailor Sumit", "7894561230", "hash", Role.TAILOR);
        tailorUser.setId(2L);
    }

    // Helper method to setup an order with an item
    private CustomerOrder createOrderWithItem(Long orderId, Long itemId, BigDecimal itemPrice, BigDecimal advancePaid) {
        Customer customer = new Customer("Rajesh", "9876500000");
        CustomerOrder order = new CustomerOrder(1001L, customer, ownerUser, fixedClock.instant());
        order.setId(orderId);

        Garment shirt = new Garment("Shirt", 1, true);
        OrderItem item = new OrderItem(order, shirt, null, "Standard", itemPrice, LocalDate.of(2026, 10, 20));
        item.setId(itemId);
        order.getItems().add(item);

        if (advancePaid != null && advancePaid.compareTo(BigDecimal.ZERO) > 0) {
            Payment payment = new Payment(order, advancePaid, PaymentMode.CASH.name(), LocalDate.of(2026, 10, 14));
            order.getPayments().add(payment);
        }

        return order;
    }

    @Test
    @DisplayName("deliverItem succeeds when order balance is zero")
    void testDeliverItem_balanceZero_deliversSuccessfully() {
        Long orderId = 10L;
        Long itemId = 25L;
        // Total Rs. 1000, paid Rs. 1000 -> balance 0
        CustomerOrder order = createOrderWithItem(orderId, itemId, new BigDecimal("1000.00"), new BigDecimal("1000.00"));
        OrderItem item = order.getItems().get(0);

        when(customerOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderItem result = deliveryService.deliverItem(orderId, itemId);

        assertEquals(ItemStatus.DELIVERED, result.getStatus());
        assertEquals(fixedClock.instant(), result.getDeliveredAt());
        verify(orderItemRepository).save(item);
    }

    @Test
    @DisplayName("deliverItem throws BalancePendingException when order balance is positive")
    void testDeliverItem_balancePositive_throwsBalancePendingException() {
        Long orderId = 10L;
        Long itemId = 25L;
        // Total Rs. 1500, paid Rs. 500 -> balance Rs. 1000
        CustomerOrder order = createOrderWithItem(orderId, itemId, new BigDecimal("1500.00"), new BigDecimal("500.00"));
        OrderItem item = order.getItems().get(0);

        when(customerOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        BalancePendingException ex = assertThrows(
                BalancePendingException.class,
                () -> deliveryService.deliverItem(orderId, itemId)
        );

        assertEquals(new BigDecimal("1000.00"), ex.getBalance());
        assertEquals(itemId, ex.getItemId());
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("deliverItem throws BusinessRuleException when item is already DELIVERED")
    void testDeliverItem_alreadyDelivered_throwsBusinessRuleException() {
        Long orderId = 10L;
        Long itemId = 25L;
        // Balance 0, but item is already delivered
        CustomerOrder order = createOrderWithItem(orderId, itemId, new BigDecimal("800.00"), new BigDecimal("800.00"));
        OrderItem item = order.getItems().get(0);
        item.markDelivered(fixedClock.instant());

        when(customerOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> deliveryService.deliverItem(orderId, itemId)
        );

        assertTrue(ex.getMessage().contains("already DELIVERED"));
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("deliverWithOverride saves DeliveryOverride audit row and marks item DELIVERED when called by OWNER")
    void testDeliverWithOverride_asOwner_savesOverrideAndDelivers() {
        Long orderId = 10L;
        Long itemId = 25L;
        // Total Rs. 1200, paid Rs. 400 -> balance Rs. 800
        CustomerOrder order = createOrderWithItem(orderId, itemId, new BigDecimal("1200.00"), new BigDecimal("400.00"));
        OrderItem item = order.getItems().get(0);

        when(customerOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        String reason = "Regular loyal customer, promises to pay via UPI tomorrow evening";
        OrderItem result = deliveryService.deliverWithOverride(orderId, itemId, reason, ownerUser);

        assertEquals(ItemStatus.DELIVERED, result.getStatus());
        assertEquals(fixedClock.instant(), result.getDeliveredAt());

        // Verify audit row saved with balance at time and owner identity
        ArgumentCaptor<DeliveryOverride> overrideCaptor = ArgumentCaptor.forClass(DeliveryOverride.class);
        verify(deliveryOverrideRepository).save(overrideCaptor.capture());
        DeliveryOverride savedOverride = overrideCaptor.getValue();

        assertEquals(new BigDecimal("800.00"), savedOverride.getBalanceAtTime());
        assertEquals(reason, savedOverride.getReason());
        assertEquals(ownerUser, savedOverride.getOverriddenBy());
        assertEquals(item, savedOverride.getOrderItem());
    }

    @Test
    @DisplayName("deliverWithOverride throws AccessDeniedException when called by non-owner TAILOR")
    void testDeliverWithOverride_asTailor_throwsAccessDeniedException() {
        Long orderId = 10L;
        Long itemId = 25L;

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> deliveryService.deliverWithOverride(orderId, itemId, "Friend of mine", tailorUser)
        );

        assertTrue(ex.getMessage().contains("Only shop OWNER"));
        verify(deliveryOverrideRepository, never()).save(any());
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("deliverWithOverride throws BusinessRuleException when reason is blank")
    void testDeliverWithOverride_blankReason_throwsBusinessRuleException() {
        Long orderId = 10L;
        Long itemId = 25L;

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> deliveryService.deliverWithOverride(orderId, itemId, "   ", ownerUser)
        );

        assertTrue(ex.getMessage().contains("cannot be blank"));
        verify(deliveryOverrideRepository, never()).save(any());
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("deliverWithOverride throws BusinessRuleException when reason exceeds 500 characters")
    void testDeliverWithOverride_reasonTooLong_throwsBusinessRuleException() {
        Long orderId = 10L;
        Long itemId = 25L;
        String longReason = "A".repeat(501);

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> deliveryService.deliverWithOverride(orderId, itemId, longReason, ownerUser)
        );

        assertTrue(ex.getMessage().contains("cannot exceed 500 characters"));
        verify(deliveryOverrideRepository, never()).save(any());
        verify(orderItemRepository, never()).save(any());
    }
}
