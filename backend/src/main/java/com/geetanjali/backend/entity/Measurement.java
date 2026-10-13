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
import java.time.Instant;

// Measurement Entity: Represents a measurement session for a customer and a specific garment.
// For example, "Ramesh's measurements for a Shirt taken on Oct 12".
// @Entity = Tells JPA/Hibernate that this class maps to a relational database table.
@Entity
// @Table = Specifies table name in PostgreSQL ("measurement").
@Table(name = "measurement")
public class Measurement {

    // @Id = Marks this field as the primary key.
    // @GeneratedValue = Auto-increments using PostgreSQL IDENTITY column.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @ManyToOne = Many measurements belong to one customer.
    // fetch = FetchType.LAZY = Don't load customer from DB until accessed.
    // @JoinColumn = Foreign key column "customer_id".
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // @ManyToOne = Many measurements reference one garment type.
    // fetch = FetchType.LAZY = Don't load garment from DB until accessed.
    // @JoinColumn = Foreign key column "garment_id".
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "garment_id", nullable = false)
    private Garment garment;

    // @Column = Timestamp when the measurement was taken (stored as TIMESTAMPTZ in PostgreSQL, mapped to java.time.Instant).
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // Default zero-argument constructor: JPA/Hibernate requires this.
    public Measurement() {
    }

    // Convenience constructor for creating a measurement record.
    // Inputs: customer (Customer), garment (Garment), createdAt (Instant).
    // Output: New Measurement instance.
    public Measurement(Customer customer, Garment garment, Instant createdAt) {
        this.customer = customer;
        this.garment = garment;
        this.createdAt = createdAt;
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

    // Getter for customer.
    // Input: None.
    // Output: Customer.
    public Customer getCustomer() {
        return customer;
    }

    // Setter for customer.
    // Input: Customer customer.
    // Output: None (void).
    public void setCustomer(Customer customer) {
        this.customer = customer;
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

    // Getter for createdAt.
    // Input: None.
    // Output: Instant (timestamptz).
    public Instant getCreatedAt() {
        return createdAt;
    }

    // Setter for createdAt.
    // Input: Instant createdAt.
    // Output: None (void).
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
