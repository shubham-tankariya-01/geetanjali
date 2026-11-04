package com.geetanjali.backend.repository;

import com.geetanjali.backend.entity.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

// CustomerOrderRepository: Database access interface for customer orders.
// @Repository = Tells Spring to manage this interface as a data access bean.
@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    // Retrieves the next human-readable order number from PostgreSQL sequence `order_no_seq` (e.g. 1001, 1002).
    // Input: None.
    // Output: Long - next order sequence value.
    @Query(value = "SELECT nextval('order_no_seq')", nativeQuery = true)
    Long getNextOrderNo();

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

    // Retrieves up to 20 most recent orders across the entire tailor shop, sorted newest first.
    // Derived query: Spring Data generates `SELECT * FROM customer_order ORDER BY created_at DESC LIMIT 20`.
    // In Express, this is like `CustomerOrder.find().sort({ createdAt: -1 }).limit(20)`.
    // Input: None.
    // Output: List<CustomerOrder> - top 20 recent orders.
    List<CustomerOrder> findTop20ByOrderByCreatedAtDesc();

    // Finds all orders booked by a specific staff member or dealer, newest first.
    // Input: createdById (Long).
    // Output: List<CustomerOrder>.
    List<CustomerOrder> findByCreatedByIdOrderByCreatedAtDesc(Long createdById);

    // Counts total orders booked by a specific staff member or dealer.
    // Input: createdById (Long).
    // Output: long.
    long countByCreatedById(Long createdById);
}
