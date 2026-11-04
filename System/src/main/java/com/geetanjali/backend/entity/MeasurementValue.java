package com.geetanjali.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.math.BigDecimal;

// MeasurementValue Entity: Represents a specific measured numeric value for a garment field.
// For example: Chest = 40.5 inches in Measurement session #12.
// @Entity = Tells JPA/Hibernate that this class maps to a relational database table.
@Entity
// @Table = Specifies the table name in PostgreSQL ("measurement_value").
@Table(name = "measurement_value")
public class MeasurementValue {

    // @EmbeddedId = Specifies that the primary key is composite, defined by the MeasurementValueId class.
    @EmbeddedId
    private MeasurementValueId id = new MeasurementValueId();

    // @ManyToOne = References the parent Measurement entity.
    // fetch = FetchType.LAZY = Don't load full Measurement row until accessed.
    // @MapsId("measurementId") = Binds this relationship to the measurementId field inside the @EmbeddedId key.
    // @JoinColumn = Foreign key column "measurement_id".
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("measurementId")
    @JoinColumn(name = "measurement_id")
    private Measurement measurement;

    // @ManyToOne = References the GarmentField definition (e.g., Chest).
    // fetch = FetchType.LAZY = Don't load full GarmentField row until accessed.
    // @MapsId("fieldId") = Binds this relationship to the fieldId field inside the @EmbeddedId key.
    // @JoinColumn = Foreign key column "field_id".
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("fieldId")
    @JoinColumn(name = "field_id")
    private GarmentField field;

    // @Column = The actual numeric value recorded (e.g., 42.00). Stored as NUMERIC in DB, mapped to BigDecimal in Java.
    @Column(name = "value", nullable = false)
    private BigDecimal value;

    // Default zero-argument constructor: JPA/Hibernate requires this.
    public MeasurementValue() {
    }

    // Convenience constructor for creating a measurement value.
    // Inputs: measurement (Measurement), field (GarmentField), value (BigDecimal).
    // Output: New MeasurementValue instance.
    public MeasurementValue(Measurement measurement, GarmentField field, BigDecimal value) {
        this.measurement = measurement;
        this.field = field;
        this.value = value;
        if (measurement != null && field != null) {
            this.id = new MeasurementValueId(measurement.getId(), field.getId());
        }
    }

    // Getter for composite id.
    // Input: None.
    // Output: MeasurementValueId.
    public MeasurementValueId getId() {
        return id;
    }

    // Setter for composite id.
    // Input: MeasurementValueId id.
    // Output: None (void).
    public void setId(MeasurementValueId id) {
        this.id = id;
    }

    // Getter for measurement.
    // Input: None.
    // Output: Measurement.
    public Measurement getMeasurement() {
        return measurement;
    }

    // Setter for measurement.
    // Input: Measurement measurement.
    // Output: None (void).
    public void setMeasurement(Measurement measurement) {
        this.measurement = measurement;
        if (measurement != null) {
            if (this.id == null) {
                this.id = new MeasurementValueId();
            }
            this.id.setMeasurementId(measurement.getId());
        }
    }

    // Getter for garment field.
    // Input: None.
    // Output: GarmentField.
    public GarmentField getField() {
        return field;
    }

    // Setter for garment field.
    // Input: GarmentField field.
    // Output: None (void).
    public void setField(GarmentField field) {
        this.field = field;
        if (field != null) {
            if (this.id == null) {
                this.id = new MeasurementValueId();
            }
            this.id.setFieldId(field.getId());
        }
    }

    // Getter for value.
    // Input: None.
    // Output: BigDecimal.
    public BigDecimal getValue() {
        return value;
    }

    // Setter for value.
    // Input: BigDecimal value.
    // Output: None (void).
    public void setValue(BigDecimal value) {
        this.value = value;
    }
}
