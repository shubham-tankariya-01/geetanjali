package com.geetanjali.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// GarmentField Entity: Represents a configurable measurement field for a garment (e.g., "Chest", "Waist", "Length").
// In Prisma, this is like a relational child model `GarmentField` belonging to `Garment`.
// @Entity = Tells JPA/Hibernate that this class maps to a relational database table.
@Entity
// @Table = Specifies the table name in PostgreSQL ("garment_field").
@Table(name = "garment_field")
public class GarmentField {

    // @Id = Marks this field as the primary key.
    // @GeneratedValue = Auto-increments using PostgreSQL IDENTITY column.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @ManyToOne = Many fields belong to one garment.
    // fetch = FetchType.LAZY = Don't fetch the parent Garment row until explicitly requested.
    // @JoinColumn = Specifies foreign key column "garment_id".
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "garment_id", nullable = false)
    private Garment garment;

    // @Column = Label shown to the tailor, e.g. "Chest".
    @Column(name = "label", nullable = false)
    private String label;

    // @Column = Unit of measurement, defaults to "inch".
    @Column(name = "unit", nullable = false)
    private String unit = "inch";

    // @Column = Order of appearance on the measurement taking form.
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    // @Column = Active flag to enable/disable specific measurement fields.
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    // Default zero-argument constructor: JPA/Hibernate requires this.
    public GarmentField() {
    }

    // Convenience constructor for creating a field.
    // Inputs: garment (Garment), label (String), unit (String), sortOrder (Integer), active (Boolean).
    // Output: New GarmentField instance.
    public GarmentField(Garment garment, String label, String unit, Integer sortOrder, Boolean active) {
        this.garment = garment;
        this.label = label;
        this.unit = unit;
        this.sortOrder = sortOrder;
        this.active = active;
    }

    // Getter for id.
    // Input: None.
    // Output: Long (primary key).
    public Long getId() {
        return id;
    }

    // Setter for id.
    // Input: Long id.
    // Output: None (void).
    public void setId(Long id) {
        this.id = id;
    }

    // Getter for garment.
    // Input: None.
    // Output: Garment.
    public Garment getGarment() {
        return garment;
    }

    // Setter for garment.
    // Input: Garment garment.
    // Output: None (void).
    public void setGarment(Garment garment) {
        this.garment = garment;
    }

    // Getter for label.
    // Input: None.
    // Output: String.
    public String getLabel() {
        return label;
    }

    // Setter for label.
    // Input: String label.
    // Output: None (void).
    public void setLabel(String label) {
        this.label = label;
    }

    // Getter for unit.
    // Input: None.
    // Output: String.
    public String getUnit() {
        return unit;
    }

    // Setter for unit.
    // Input: String unit.
    // Output: None (void).
    public void setUnit(String unit) {
        this.unit = unit;
    }

    // Getter for sortOrder.
    // Input: None.
    // Output: Integer.
    public Integer getSortOrder() {
        return sortOrder;
    }

    // Setter for sortOrder.
    // Input: Integer sortOrder.
    // Output: None (void).
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    // Getter for active.
    // Input: None.
    // Output: Boolean.
    public Boolean getActive() {
        return active;
    }

    // Setter for active.
    // Input: Boolean active.
    // Output: None (void).
    public void setActive(Boolean active) {
        this.active = active;
    }
}
