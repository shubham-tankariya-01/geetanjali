package com.geetanjali.backend.repository;

import com.geetanjali.backend.entity.Customer;
import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.entity.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

// MeasurementRepository: Database access interface for customer measurement sessions.
// Each row represents a point-in-time measurement snapshot for a specific customer and garment type.
//
// @Repository = Tells Spring to instantiate this interface as a Spring Data JPA repository bean.
@Repository
public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

    // Finds all measurement snapshots for a customer ordered by newest first.
    // Inputs: customerId (Long) - customer primary key.
    // Output: List<Measurement> - list of all measurement sessions, latest first.
    List<Measurement> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    // Finds the newest (most recent) measurement snapshot for a specific customer and garment using IDs.
    // Spring Data derives the SQL query:
    // SELECT * FROM measurement WHERE customer_id = ? AND garment_id = ? ORDER BY created_at DESC LIMIT 1.
    //
    // Inputs: customerId (Long) - customer primary key, garmentId (Long) - garment primary key.
    // Output: Optional<Measurement> - the latest snapshot, or empty if none exists.
    Optional<Measurement> findFirstByCustomerIdAndGarmentIdOrderByCreatedAtDesc(Long customerId, Long garmentId);

    // Convenience method: Finds the newest measurement snapshot using entity objects directly.
    // Inputs: customer (Customer) - customer entity, garment (Garment) - garment entity.
    // Output: Optional<Measurement> - the latest snapshot, or empty if none exists.
    Optional<Measurement> findFirstByCustomerAndGarmentOrderByCreatedAtDesc(Customer customer, Garment garment);
}
