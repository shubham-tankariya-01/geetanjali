package com.geetanjali.backend.repository;

import com.geetanjali.backend.entity.MeasurementValue;
import com.geetanjali.backend.entity.MeasurementValueId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

// MeasurementValueRepository: Database access interface for individual measurement field values.
// Uses composite primary key MeasurementValueId.
// @Repository = Tells Spring to manage this interface as a data access bean.
@Repository
public interface MeasurementValueRepository extends JpaRepository<MeasurementValue, MeasurementValueId> {

    // Finds all values recorded in a specific measurement session.
    // Input: measurementId (Long).
    // Output: List<MeasurementValue>.
    List<MeasurementValue> findByMeasurementId(Long measurementId);
}
