package com.geetanjali.backend.repository;

import com.geetanjali.backend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

// OrderItemRepository: Database access interface for individual items within an order.
// @Repository = Tells Spring to manage this interface as a data access bean.
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Finds all items belonging to a specific order.
    // Input: orderId (Long).
    // Output: List<OrderItem>.
    List<OrderItem> findByOrderId(Long orderId);

    // Finds all items by their status (e.g. 'PENDING', 'DONE', 'DELIVERED').
    // Input: status (String).
    // Output: List<OrderItem>.
    List<OrderItem> findByStatus(String status);
}
