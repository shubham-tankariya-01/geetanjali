package com.geetanjali.backend.repository;

import com.geetanjali.backend.entity.DeliveryOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

// DeliveryOverrideRepository: Database access interface for delivery override audit records.
// @Repository = Tells Spring to manage this interface as a data access bean.
@Repository
public interface DeliveryOverrideRepository extends JpaRepository<DeliveryOverride, Long> {

    // Finds the delivery override record for a specific order item.
    // Input: orderItemId (Long).
    // Output: Optional<DeliveryOverride>.
    Optional<DeliveryOverride> findByOrderItemId(Long orderItemId);
}
