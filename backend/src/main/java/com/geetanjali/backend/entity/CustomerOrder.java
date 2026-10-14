package com.geetanjali.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// CustomerOrder Entity: Represents an overarching tailor order created for a customer.
// In Express, this is the main Order model that ties items and payments together.
// @Entity = Tells JPA/Hibernate that this class maps to a relational database table.
@Entity
// @Table = Specifies table name in PostgreSQL ("customer_order").
@Table(name = "customer_order")
public class CustomerOrder {

    // @Id = Marks this field as the primary key.
    // @GeneratedValue = Auto-increments using PostgreSQL IDENTITY column.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column = Unique human-friendly order sequence number (e.g., 1001, 1002).
    @Column(name = "order_no", nullable = false, unique = true)
    private Long orderNo;

    // @ManyToOne = Many orders can belong to one customer.
    // fetch = FetchType.LAZY = Don't fetch customer details until needed.
    // @JoinColumn = Foreign key column "customer_id".
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // @ManyToOne = Many orders can be created by a specific staff member/user.
    // fetch = FetchType.LAZY = Don't fetch user details until needed.
    // @JoinColumn = Foreign key column "created_by".
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    // @Column = Timestamp when order was placed.
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // @OneToMany = One customer order contains multiple order garment items.
    // mappedBy = "order" = Specifies the field in OrderItem that owns the relationship.
    // fetch = FetchType.LAZY = Loaded on demand when items are accessed.
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    // @OneToMany = One customer order can receive multiple payments (advance + delivery balance).
    // mappedBy = "order" = Specifies the field in Payment that owns the relationship.
    // fetch = FetchType.LAZY = Loaded on demand when payments are accessed.
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<Payment> payments = new ArrayList<>();

    // Default zero-argument constructor: JPA/Hibernate requires this.
    public CustomerOrder() {
    }

    // Convenience constructor for creating a customer order.
    // Inputs: orderNo (Long), customer (Customer), createdBy (AppUser), createdAt (Instant).
    // Output: New CustomerOrder instance.
    public CustomerOrder(Long orderNo, Customer customer, AppUser createdBy, Instant createdAt) {
        this.orderNo = orderNo;
        this.customer = customer;
        this.createdBy = createdBy;
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

    // Getter for orderNo.
    // Input: None.
    // Output: Long (order number).
    public Long getOrderNo() {
        return orderNo;
    }

    // Setter for orderNo.
    // Input: Long orderNo.
    // Output: None (void).
    public void setOrderNo(Long orderNo) {
        this.orderNo = orderNo;
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

    // Getter for createdBy.
    // Input: None.
    // Output: AppUser.
    public AppUser getCreatedBy() {
        return createdBy;
    }

    // Setter for createdBy.
    // Input: AppUser createdBy.
    // Output: None (void).
    public void setCreatedBy(AppUser createdBy) {
        this.createdBy = createdBy;
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

    // Getter for items.
    // Input: None.
    // Output: List<OrderItem>.
    public List<OrderItem> getItems() {
        return items;
    }

    // Setter for items.
    // Input: List<OrderItem> items.
    // Output: None (void).
    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    // Getter for payments.
    // Input: None.
    // Output: List<Payment>.
    public List<Payment> getPayments() {
        return payments;
    }

    // Setter for payments.
    // Input: List<Payment> payments.
    // Output: None (void).
    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    // =========================================================================
    // CALCULATED GETTERS (Stage 2: Pure arithmetic, no database queries inside)
    // =========================================================================

    // Calculates the total cost of the order by summing all item prices.
    // Pure arithmetic: Operates strictly in-memory over the items collection.
    // Why calculated instead of stored in DB? Storing total introduces duplicate state that can get out of sync if items change.
    // Input: None.
    // Output: BigDecimal sum of item prices, or BigDecimal.ZERO if order has no items.
    public BigDecimal getTotal() {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (OrderItem item : items) {
            if (item.getPrice() != null) {
                sum = sum.add(item.getPrice());
            }
        }
        return sum;
    }

    // Calculates total money collected so far by summing all payment receipts.
    // Pure arithmetic: Sums payment amounts in memory.
    // Input: None.
    // Output: BigDecimal sum of payments, or BigDecimal.ZERO if no payments exist.
    public BigDecimal getPaid() {
        if (payments == null || payments.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (Payment payment : payments) {
            if (payment.getAmount() != null) {
                sum = sum.add(payment.getAmount());
            }
        }
        return sum;
    }

    // Calculates outstanding balance due for this order (total - paid).
    // Pure arithmetic: Subtracts total paid from total item price.
    // Why calculated? Balance is a derived financial ledger concept; deriving it guarantees zero ledger drift.
    // Input: None.
    // Output: BigDecimal balance (may be negative if customer overpaid).
    public BigDecimal getBalance() {
        return getTotal().subtract(getPaid());
    }

    // Checks whether this order is fully settled.
    // Pure arithmetic: True if remaining balance is zero or negative (balance <= 0).
    // Input: None.
    // Output: boolean true if balance <= 0, false otherwise.
    public boolean isPaid() {
        return getBalance().compareTo(BigDecimal.ZERO) <= 0;
    }

    // Backwards-compatible alias for getTotal() used by Stage 1 templates and tests.
    // Input: None.
    // Output: BigDecimal total amount.
    public BigDecimal getTotalAmount() {
        return getTotal();
    }

    // Backwards-compatible alias for getPaid() used by Stage 1 templates and tests.
    // Input: None.
    // Output: BigDecimal total paid.
    public BigDecimal getTotalPaid() {
        return getPaid();
    }

    // Backwards-compatible alias for getBalance() clamped to zero for display.
    // Input: None.
    // Output: BigDecimal balance due (minimum 0).
    public BigDecimal getBalanceDue() {
        return getBalance().max(BigDecimal.ZERO);
    }

    // Returns total count of garment items in this order.
    // Input: None.
    // Output: int - number of items.
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    // Determines the nearest (earliest) promised delivery date among all items in this order.
    // Used by Home dashboard to highlight upcoming deadlines for the tailor.
    // Input: None.
    // Output: LocalDate - earliest delivery date, or null if order has no items.
    public LocalDate getNearestDeliveryDate() {
        if (items == null || items.isEmpty()) {
            return null;
        }
        LocalDate nearest = null;
        for (OrderItem item : items) {
            LocalDate date = item.getDeliveryDate();
            if (date != null) {
                if (nearest == null || date.isBefore(nearest)) {
                    nearest = date;
                }
            }
        }
        return nearest;
    }
}
