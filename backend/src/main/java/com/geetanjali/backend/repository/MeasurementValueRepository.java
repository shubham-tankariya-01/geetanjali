package com.geetanjali.backend.repository;

import com.geetanjali.backend.entity.Measurement;
import com.geetanjali.backend.entity.MeasurementValue;
import com.geetanjali.backend.entity.MeasurementValueId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

// MeasurementValueRepository: Database access interface for individual measurement field values.
// Operates on the composite-key table "measurement_value" using composite key MeasurementValueId (measurement_id, field_id).
//
// @Repository = Tells Spring to manage this interface as a data access repository bean.
@Repository
public interface MeasurementValueRepository extends JpaRepository<MeasurementValue, MeasurementValueId> {

    // Finds all measurement values belonging to a specific measurement snapshot by ID.
    // Inputs: measurementId (Long) - the ID of the measurement snapshot.
    // Output: List<MeasurementValue> - all recorded field numbers for this snapshot.
    List<MeasurementValue> findByMeasurementId(Long measurementId);

    // Convenience method: Finds all measurement values belonging to a measurement snapshot entity.
    // Inputs: measurement (Measurement) - parent measurement entity.
    // Output: List<MeasurementValue> - all recorded field numbers for this snapshot.
    List<MeasurementValue> findByMeasurement(Measurement measurement);
}
