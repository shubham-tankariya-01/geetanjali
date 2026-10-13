package com.geetanjali.backend.repository;

import com.geetanjali.backend.entity.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

// CustomerOrderRepository: Database access interface for customer orders.
// @Repository = Tells Spring to manage this interface as a data access bean.
@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    // Finds an order by its human-friendly unique order number (e.g. 1001).
    // Input: orderNo (Long).
    // Output: Optional<CustomerOrder>.
    Optional<CustomerOrder> findByOrderNo(Long orderNo);

    // Finds all orders belonging to a customer ordered by most recent first.
    // Input: customerId (Long).
    // Output: List<CustomerOrder>.
    List<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    // Counts the total number of orders placed by a customer.
    // Derived query: Spring Data generates `SELECT count(*) FROM customer_order WHERE customer_id = ?`.
    // In Express, this is like `CustomerOrder.countDocuments({ customerId })`.
    // Input: customerId (Long).
    // Output: long - total count of earlier orders.
    long countByCustomerId(Long customerId);

    // Finds the single most recent order placed by a customer.
    // Derived query: Spring Data generates `SELECT * FROM customer_order WHERE customer_id = ? ORDER BY created_at DESC LIMIT 1`.
    // Input: customerId (Long).
    // Output: Optional<CustomerOrder> - the latest order, or empty if customer has never ordered.
    Optional<CustomerOrder> findFirstByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
