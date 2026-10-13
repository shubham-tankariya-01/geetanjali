package com.geetanjali.backend.repository;

import com.geetanjali.backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

// PaymentRepository: Database access interface for customer payments and advances.
// @Repository = Tells Spring to manage this interface as a data access bean.
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Finds all payments made towards a specific order.
    // Input: orderId (Long).
    // Output: List<Payment>.
    List<Payment> findByOrderId(Long orderId);
}
