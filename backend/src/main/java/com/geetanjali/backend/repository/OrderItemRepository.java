package com.geetanjali.backend.repository;

import com.geetanjali.backend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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

    // Counts distinct orders scheduled for delivery on a given date.
    @Query("SELECT COUNT(DISTINCT i.order.id) FROM OrderItem i WHERE i.deliveryDate = :deliveryDate")
    long countDistinctOrdersByDeliveryDate(@Param("deliveryDate") LocalDate deliveryDate);

    // Counts total garment items scheduled for delivery on a given date.
    @Query("SELECT COUNT(i.id) FROM OrderItem i WHERE i.deliveryDate = :deliveryDate")
    long countItemsByDeliveryDate(@Param("deliveryDate") LocalDate deliveryDate);

    // Retrieves all order items scheduled between two delivery dates (inclusive),
    // eager fetching order, customer, and garment to eliminate N+1 queries.
    @Query("SELECT i FROM OrderItem i " +
           "JOIN FETCH i.order o " +
           "JOIN FETCH o.customer c " +
           "JOIN FETCH i.garment g " +
           "WHERE i.deliveryDate BETWEEN :startDate AND :endDate " +
           "ORDER BY i.deliveryDate ASC, o.orderNo ASC")
    List<OrderItem> findItemsBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Retrieves all items scheduled for delivery on a specific date.
    @Query("SELECT i FROM OrderItem i " +
           "JOIN FETCH i.order o " +
           "JOIN FETCH o.customer c " +
           "JOIN FETCH i.garment g " +
           "WHERE i.deliveryDate = :deliveryDate " +
           "ORDER BY o.orderNo ASC, i.id ASC")
    List<OrderItem> findItemsByDeliveryDate(@Param("deliveryDate") LocalDate deliveryDate);
}

