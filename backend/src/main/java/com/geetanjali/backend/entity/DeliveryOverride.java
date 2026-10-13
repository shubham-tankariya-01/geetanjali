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

// DeliveryOverride Entity: Records an audit trail when an item is delivered with an unpaid balance.
// For example: A loyal regular customer takes clothes home and will pay via UPI tomorrow.
// @Entity = Tells JPA/Hibernate that this class maps to a relational database table.
@Entity
// @Table = Specifies table name in PostgreSQL ("delivery_override").
@Table(name = "delivery_override")
public class DeliveryOverride {

    // @Id = Marks this field as the primary key.
    // @GeneratedValue = Auto-increments using PostgreSQL IDENTITY column.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @ManyToOne = References the delivered order item.
    // fetch = FetchType.LAZY = Don't fetch the order item details until accessed.
    // @JoinColumn = Foreign key column "order_item_id".
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    // @Column = Pending balance amount at the moment the item was handed over.
    @Column(name = "balance_at_time", nullable = false)
    private BigDecimal balanceAtTime;

    // @Column = Tailor or shop owner's justification (e.g. "Trusted neighbor, will settle on Saturday").
    @Column(name = "reason", nullable = false)
    private String reason;

    // @ManyToOne = Staff member who authorized the delivery without full payment.
    // fetch = FetchType.LAZY = Don't fetch user row until accessed.
    // @JoinColumn = Foreign key column "overridden_by".
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "overridden_by", nullable = false)
    private AppUser overriddenBy;

    // Default zero-argument constructor: JPA/Hibernate requires this.
    public DeliveryOverride() {
    }

    // Convenience constructor for creating a delivery override audit entry.
    // Inputs: orderItem (OrderItem), balanceAtTime (BigDecimal), reason (String), overriddenBy (AppUser).
    // Output: New DeliveryOverride instance.
    public DeliveryOverride(OrderItem orderItem, BigDecimal balanceAtTime, String reason, AppUser overriddenBy) {
        this.orderItem = orderItem;
        this.balanceAtTime = balanceAtTime;
        this.reason = reason;
        this.overriddenBy = overriddenBy;
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

    // Getter for orderItem.
    // Input: None.
    // Output: OrderItem.
    public OrderItem getOrderItem() {
        return orderItem;
    }

    // Setter for orderItem.
    // Input: OrderItem orderItem.
    // Output: None (void).
    public void setOrderItem(OrderItem orderItem) {
        this.orderItem = orderItem;
    }

    // Getter for balanceAtTime.
    // Input: None.
    // Output: BigDecimal.
    public BigDecimal getBalanceAtTime() {
        return balanceAtTime;
    }

    // Setter for balanceAtTime.
    // Input: BigDecimal balanceAtTime.
    // Output: None (void).
    public void setBalanceAtTime(BigDecimal balanceAtTime) {
        this.balanceAtTime = balanceAtTime;
    }

    // Getter for reason.
    // Input: None.
    // Output: String.
    public String getReason() {
        return reason;
    }

    // Setter for reason.
    // Input: String reason.
    // Output: None (void).
    public void setReason(String reason) {
        this.reason = reason;
    }

    // Getter for overriddenBy.
    // Input: None.
    // Output: AppUser.
    public AppUser getOverriddenBy() {
        return overriddenBy;
    }

    // Setter for overriddenBy.
    // Input: AppUser overriddenBy.
    // Output: None (void).
    public void setOverriddenBy(AppUser overriddenBy) {
        this.overriddenBy = overriddenBy;
    }
}
