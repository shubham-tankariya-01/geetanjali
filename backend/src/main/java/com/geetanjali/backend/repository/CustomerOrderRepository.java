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
}
