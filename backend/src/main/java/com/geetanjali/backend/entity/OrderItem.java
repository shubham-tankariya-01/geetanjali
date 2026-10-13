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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

// OrderItem Entity: Represents an individual clothing item within a customer order.
// For example: 1 Shirt with custom notes, delivery date Oct 20, and price 450.00.
// @Entity = Tells JPA/Hibernate that this class maps to a relational database table.
@Entity
// @Table = Specifies table name in PostgreSQL ("order_item").
@Table(name = "order_item")
public class OrderItem {

    // @Id = Marks this field as the primary key.
    // @GeneratedValue = Auto-increments using PostgreSQL IDENTITY column.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @ManyToOne = Many order items belong to one customer order.
    // fetch = FetchType.LAZY = Don't fetch the parent order row until accessed.
    // @JoinColumn = Foreign key column "order_id".
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    // @ManyToOne = Many items can be of a specific garment type.
    // fetch = FetchType.LAZY = Don't fetch garment details until accessed.
    // @JoinColumn = Foreign key column "garment_id".
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "garment_id", nullable = false)
    private Garment garment;

    // @ManyToOne = Optional link to the specific measurement snapshot used for stitching this item.
    // fetch = FetchType.LAZY = Don't fetch measurement details until accessed.
    // @JoinColumn = Foreign key column "measurement_id" (nullable because measurements might be recorded later).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "measurement_id")
    private Measurement measurement;

    // @Column = Custom stitching instructions (e.g. "Double pocket, slim fit, Chinese collar").
    @Column(name = "notes")
    private String notes;

    // @Column = Stitching charge / price for this item. BigDecimal ensures exact decimal precision for money.
    @Column(name = "price", nullable = false)
    private BigDecimal price;

    // @Column = Promised delivery date for the customer.
    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    // @Column = Lifecycle status: 'PENDING' (default), 'DONE' (stitched), or 'DELIVERED' (handed to customer).
    @Column(name = "status", nullable = false)
    private String status = "PENDING";

    // @Column = Timestamp when stitching was marked complete.
    @Column(name = "done_at")
    private Instant doneAt;

    // @Column = Timestamp when item was delivered to customer.
    @Column(name = "delivered_at")
    private Instant deliveredAt;

    // Default zero-argument constructor: JPA/Hibernate requires this.
    public OrderItem() {
    }

    // Convenience constructor for creating an order item.
    // Inputs: order (CustomerOrder), garment (Garment), measurement (Measurement), notes (String), price (BigDecimal), deliveryDate (LocalDate).
    // Output: New OrderItem instance.
    public OrderItem(CustomerOrder order, Garment garment, Measurement measurement, String notes, BigDecimal price, LocalDate deliveryDate) {
        this.order = order;
        this.garment = garment;
        this.measurement = measurement;
        this.notes = notes;
        this.price = price;
        this.deliveryDate = deliveryDate;
        this.status = "PENDING";
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

    // Getter for order.
    // Input: None.
    // Output: CustomerOrder.
    public CustomerOrder getOrder() {
        return order;
    }

    // Setter for order.
    // Input: CustomerOrder order.
    // Output: None (void).
    public void setOrder(CustomerOrder order) {
        this.order = order;
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
    }

    // Getter for notes.
    // Input: None.
    // Output: String.
    public String getNotes() {
        return notes;
    }

    // Setter for notes.
    // Input: String notes.
    // Output: None (void).
    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Getter for price.
    // Input: None.
    // Output: BigDecimal.
    public BigDecimal getPrice() {
        return price;
    }

    // Setter for price.
    // Input: BigDecimal price.
    // Output: None (void).
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    // Getter for deliveryDate.
    // Input: None.
    // Output: LocalDate.
    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    // Setter for deliveryDate.
    // Input: LocalDate deliveryDate.
    // Output: None (void).
    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    // Getter for status.
    // Input: None.
    // Output: String ("PENDING", "DONE", "DELIVERED").
    public String getStatus() {
        return status;
    }

    // Setter for status.
    // Input: String status.
    // Output: None (void).
    public void setStatus(String status) {
        this.status = status;
    }

    // Getter for doneAt.
    // Input: None.
    // Output: Instant.
    public Instant getDoneAt() {
        return doneAt;
    }

    // Setter for doneAt.
    // Input: Instant doneAt.
    // Output: None (void).
    public void setDoneAt(Instant doneAt) {
        this.doneAt = doneAt;
    }

    // Getter for deliveredAt.
    // Input: None.
    // Output: Instant.
    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    // Setter for deliveredAt.
    // Input: Instant deliveredAt.
    // Output: None (void).
    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
}
