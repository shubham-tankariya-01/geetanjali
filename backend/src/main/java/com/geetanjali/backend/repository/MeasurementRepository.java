package com.geetanjali.backend.repository;

import com.geetanjali.backend.entity.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

// MeasurementRepository: Database access interface for customer measurement sessions.
// @Repository = Tells Spring to manage this interface as a data access bean.
@Repository
public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

    // Finds all measurements for a customer ordered by most recent first.
    // Input: customerId (Long).
    // Output: List<Measurement>.
    List<Measurement> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
