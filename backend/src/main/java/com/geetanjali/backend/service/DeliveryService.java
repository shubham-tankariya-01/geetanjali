package com.geetanjali.backend.service;

import com.geetanjali.backend.entity.AppUser;
import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.entity.DeliveryOverride;
import com.geetanjali.backend.entity.OrderItem;
import com.geetanjali.backend.exception.BalancePendingException;
import com.geetanjali.backend.exception.BusinessRuleException;
import com.geetanjali.backend.exception.ResourceNotFoundException;
import com.geetanjali.backend.repository.CustomerOrderRepository;
import com.geetanjali.backend.repository.DeliveryOverrideRepository;
import com.geetanjali.backend.repository.OrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

// DeliveryService: Manages garment delivery lifecycle, unpaid balance enforcement, and owner overrides.
// In the tailor shop workflow, clothes should not leave the shop until the customer pays in full.
// If an order has an unpaid balance, delivery is strictly blocked unless the shop OWNER explicitly overrides with a reason.
//
// @Service = Tells Spring to register this class as a business logic service bean.
@Service
public class DeliveryService {

    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryOverrideRepository deliveryOverrideRepository;
    private final Clock clock;

    // Constructor injection for repositories and the Asia/Kolkata Clock bean.
    //
    // Injects required data access repositories and clock singleton into this service.
    @Autowired
    public DeliveryService(CustomerOrderRepository customerOrderRepository,
                           OrderItemRepository orderItemRepository,
                           DeliveryOverrideRepository deliveryOverrideRepository,
                           Clock clock) {
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.deliveryOverrideRepository = deliveryOverrideRepository;
        this.clock = clock;
    }

    // Helper method to retrieve an order item and verify that it belongs to the given order.
    // Inputs: orderId (Long), itemId (Long).
    // Output: OrderItem entity.
    private OrderItem getOrderItemOrThrow(Long orderId, Long itemId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found with id: " + itemId));
        if (!item.getOrder().getId().equals(order.getId())) {
            throw new ResourceNotFoundException("Order item " + itemId + " does not belong to order " + orderId);
        }
        return item;
    }

    // Delivers a garment item normally when the order balance is zero or fully paid.
    // Preconditions:
    // 1. Order and item must exist, and item must belong to the order.
    // 2. Order balance must be zero (paid in full). If balance > 0, throws BalancePendingException.
    // 3. Item must be in PENDING or DONE status (never already DELIVERED).
    //
    // Inputs:
    //   orderId (Long) - database ID of the customer order.
    //   itemId (Long) - database ID of the garment item.
    // Output: OrderItem - updated order item marked DELIVERED.
    //
    // Ensures database changes commit atomically and roll back automatically on runtime exception.
    @Transactional
    public OrderItem deliverItem(Long orderId, Long itemId) {
        OrderItem item = getOrderItemOrThrow(orderId, itemId);
        CustomerOrder order = item.getOrder();

        // Calculate dynamic order balance (Total - Paid).
        BigDecimal balance = order.getBalance();

        // Enforce rule: if the order balance is above 0, delivery is strictly blocked.
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            throw new BalancePendingException(balance, itemId);
        }

        // Mark item delivered using current Asia/Kolkata timestamp.
        Instant now = clock.instant();
        item.markDelivered(now);
        return orderItemRepository.save(item);
    }

    // Authorizes delivery of an item despite an unpaid balance by recording an audit override entry.
    // Preconditions:
    // 1. Current user MUST have the OWNER role (server-side security check).
    // 2. Reason must not be blank and cannot exceed 500 characters.
    // 3. Item must be in PENDING or DONE status.
    //
    // Inputs:
    //   orderId (Long) - database ID of the customer order.
    //   itemId (Long) - database ID of the garment item.
    //   reason (String) - owner's justification for releasing the garment before settlement.
    //   currentUser (AppUser) - authenticated user attempting the override.
    // Output: OrderItem - updated order item marked DELIVERED with audit trail.
    //
    // Ensures both the audit row and item status are saved together in ONE atomic database transaction.
    @Transactional
    public OrderItem deliverWithOverride(Long orderId, Long itemId, String reason, AppUser currentUser) {
        // SERVER-SIDE SECURITY CHECK:
        // WHY check role on the server?
        // Hiding a button in the frontend HTML is NOT real security. Any authenticated user (including tailors)
        // could craft a POST request directly to the override endpoint. We must verify the OWNER role in Java.
        if (currentUser == null || !currentUser.isOwner()) {
            throw new AccessDeniedException("Only shop OWNER can authorize a delivery override");
        }

        // Validate override reason
        if (reason == null || reason.trim().isBlank()) {
            throw new BusinessRuleException("Override reason is required and cannot be blank");
        }
        if (reason.trim().length() > 500) {
            throw new BusinessRuleException("Override reason cannot exceed 500 characters");
        }

        OrderItem item = getOrderItemOrThrow(orderId, itemId);
        CustomerOrder order = item.getOrder();

        // Record balance at time of override handover
        BigDecimal balanceAtTime = order.getBalance();

        // Save delivery override audit row
        DeliveryOverride override = new DeliveryOverride(item, balanceAtTime, reason.trim(), currentUser);
        deliveryOverrideRepository.save(override);
        item.getDeliveryOverrides().add(override);

        // Mark item delivered
        Instant now = clock.instant();
        item.markDelivered(now);
        return orderItemRepository.save(item);
    }
}
