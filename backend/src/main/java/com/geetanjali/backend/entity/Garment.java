package com.geetanjali.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Garment Entity: Represents a category of stitched clothing (e.g., Shirt, Pant, Kurta, Blouse).
// In MongoDB, this would be a "garment" collection.
// @Entity = Tells JPA/Hibernate that this class maps to a relational database table.
@Entity
// @Table = Specifies the table name in PostgreSQL ("garment").
@Table(name = "garment")
public class Garment {

    // @Id = Marks this field as the primary key.
    // @GeneratedValue = Auto-increments using PostgreSQL IDENTITY column.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column = Garment name, e.g. "Shirt". Must be unique.
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    // @Column = Sorting order for display in dropdowns and UI lists.
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    // @Column = Soft-delete/active flag (active garments appear in selection menus).
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    // Default zero-argument constructor: JPA/Hibernate requires this.
    public Garment() {
    }

    // Convenience constructor for creating a garment.
    // Inputs: name (String), sortOrder (Integer), active (Boolean).
    // Output: New Garment instance.
    public Garment(String name, Integer sortOrder, Boolean active) {
        this.name = name;
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

    // Getter for name.
    // Input: None.
    // Output: String.
    public String getName() {
        return name;
    }

    // Setter for name.
    // Input: String name.
    // Output: None (void).
    public void setName(String name) {
        this.name = name;
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
