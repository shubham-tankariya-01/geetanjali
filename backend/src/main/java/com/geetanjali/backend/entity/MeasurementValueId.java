package com.geetanjali.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

// MeasurementValueId: Represents the composite primary key for the measurement_value table.
// In SQL, PRIMARY KEY (measurement_id, field_id) ensures one value per field per measurement session.
// @Embeddable = Tells JPA that this class can be embedded into an entity as a primary key.
@Embeddable
public class MeasurementValueId implements Serializable {

    // @Column = Maps to column "measurement_id" in the composite primary key.
    @Column(name = "measurement_id")
    private Long measurementId;

    // @Column = Maps to column "field_id" in the composite primary key.
    @Column(name = "field_id")
    private Long fieldId;

    // Default zero-argument constructor: JPA requires this.
    public MeasurementValueId() {
    }

    // Convenience constructor for creating a composite key.
    // Inputs: measurementId (Long), fieldId (Long).
    // Output: New MeasurementValueId instance.
    public MeasurementValueId(Long measurementId, Long fieldId) {
        this.measurementId = measurementId;
        this.fieldId = fieldId;
    }

    // Getter for measurementId.
    // Input: None.
    // Output: Long.
    public Long getMeasurementId() {
        return measurementId;
    }

    // Setter for measurementId.
    // Input: Long measurementId.
    // Output: None (void).
    public void setMeasurementId(Long measurementId) {
        this.measurementId = measurementId;
    }

    // Getter for fieldId.
    // Input: None.
    // Output: Long.
    public Long getFieldId() {
        return fieldId;
    }

    // Setter for fieldId.
    // Input: Long fieldId.
    // Output: None (void).
    public void setFieldId(Long fieldId) {
        this.fieldId = fieldId;
    }

    // equals method: JPA requires composite keys to correctly implement equality check.
    // Input: Object o.
    // Output: boolean (true if both measurementId and fieldId match).
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MeasurementValueId that = (MeasurementValueId) o;
        return Objects.equals(measurementId, that.measurementId) &&
               Objects.equals(fieldId, that.fieldId);
    }

    // hashCode method: JPA requires composite keys to compute a consistent hash code for caching/maps.
    // Input: None.
    // Output: int (hash code).
    @Override
    public int hashCode() {
        return Objects.hash(measurementId, fieldId);
    }
}
