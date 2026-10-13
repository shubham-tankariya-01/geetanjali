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
import java.time.LocalDate;

// Payment Entity: Records an advance or final payment received against a customer order.
// In Express, this would be a Payment collection or table linked to orderId.
// @Entity = Tells JPA/Hibernate that this class maps to a relational database table.
@Entity
// @Table = Specifies table name in PostgreSQL ("payment").
@Table(name = "payment")
public class Payment {

    // @Id = Marks this field as the primary key.
    // @GeneratedValue = Auto-increments using PostgreSQL IDENTITY column.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @ManyToOne = Many payments can be logged towards a single order (e.g. advance payment + final delivery payment).
    // fetch = FetchType.LAZY = Don't fetch the parent order row until accessed.
    // @JoinColumn = Foreign key column "order_id".
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    // @Column = Amount of money received. BigDecimal prevents floating-point rounding errors in currency calculations.
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    // @Column = Payment mode: e.g. "CASH", "UPI", "CARD".
    @Column(name = "mode", nullable = false)
    private String mode;

    // @Column = Date when money was received.
    @Column(name = "paid_on", nullable = false)
    private LocalDate paidOn = LocalDate.now();

    // Default zero-argument constructor: JPA/Hibernate requires this.
    public Payment() {
    }

    // Convenience constructor for creating a payment record.
    // Inputs: order (CustomerOrder), amount (BigDecimal), mode (String), paidOn (LocalDate).
    // Output: New Payment instance.
    public Payment(CustomerOrder order, BigDecimal amount, String mode, LocalDate paidOn) {
        this.order = order;
        this.amount = amount;
        this.mode = mode;
        this.paidOn = paidOn;
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

    // Getter for amount.
    // Input: None.
    // Output: BigDecimal.
    public BigDecimal getAmount() {
        return amount;
    }

    // Setter for amount.
    // Input: BigDecimal amount.
    // Output: None (void).
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    // Getter for mode.
    // Input: None.
    // Output: String.
    public String getMode() {
        return mode;
    }

    // Setter for mode.
    // Input: String mode.
    // Output: None (void).
    public void setMode(String mode) {
        this.mode = mode;
    }

    // Getter for paidOn.
    // Input: None.
    // Output: LocalDate.
    public LocalDate getPaidOn() {
        return paidOn;
    }

    // Setter for paidOn.
    // Input: LocalDate paidOn.
    // Output: None (void).
    public void setPaidOn(LocalDate paidOn) {
        this.paidOn = paidOn;
    }
}
