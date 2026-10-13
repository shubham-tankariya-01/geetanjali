package com.geetanjali.backend.dto.draft;

import com.geetanjali.backend.entity.Garment;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// OrderDraft: A session-scoped bean acting as the stateful shopping cart for the multi-step order wizard.
// In Express, this is the object stored in `req.session.orderDraft = { customer, garment, items: [] }`.
//
// In Spring Boot:
// @Component = Tells Spring to manage this class as a Spring Bean.
// @SessionScope = Creates a proxy bean with HTTP Session lifecycle. Each user browser gets its own separate instance,
// preserving progress across wizard screens (Step 1 -> Step 2 -> Step 3) without requiring database writes until final placement!
@Component
@SessionScope
public class OrderDraft implements Serializable {

    private static final long serialVersionUID = 1L;

    // Database primary key ID of an existing customer, or null if the customer is brand new.
    private Long customerId;

    // Customer full name (e.g. "Ramesh Patel").
    private String customerName;

    // Normalized 10-digit Indian mobile number (e.g. "9876543210").
    private String customerMobile;

    // The garment currently chosen in Step 2 for the item being configured.
    private Garment currentGarment;

    // Temporary working draft item for the current garment before adding to the items list.
    private OrderDraftItem currentItem;

    // List of completed garment items currently in the cart.
    private List<OrderDraftItem> items = new ArrayList<>();

    // Optional ID of an existing open order if adding garments to an existing order; null for a new order.
    private Long existingOrderId;

    // Default zero-argument constructor.
    public OrderDraft() {
    }

    // Resets all draft fields to start a clean, fresh order session.
    // Inputs: None.
    // Output: None (void).
    public void reset() {
        this.customerId = null;
        this.customerName = null;
        this.customerMobile = null;
        this.currentGarment = null;
        this.currentItem = null;
        this.items.clear();
        this.existingOrderId = null;
    }

    // Checks whether a customer has been identified (either by existing ID or new mobile number).
    // Inputs: None.
    // Output: boolean - true if customer info is present.
    public boolean hasCustomer() {
        return customerId != null || (customerMobile != null && !customerMobile.isBlank());
    }

    // Checks whether the chosen customer is brand new (not yet saved in PostgreSQL).
    // Inputs: None.
    // Output: boolean - true if new customer without a database ID.
    public boolean isNewCustomer() {
        return customerId == null && customerMobile != null && !customerMobile.isBlank();
    }

    // Sets customer details in the draft.
    // Inputs: customerId (Long), customerName (String), customerMobile (String).
    public void setCustomer(Long customerId, String customerName, String customerMobile) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerMobile = customerMobile;
    }

    // Adds a finished garment item to the cart.
    // Input: item (OrderDraftItem).
    public void addItem(OrderDraftItem item) {
        if (item != null) {
            this.items.add(item);
        }
    }

    // Removes an item from the cart by its index.
    // Input: index (int).
    public void removeItem(int index) {
        if (index >= 0 && index < this.items.size()) {
            this.items.remove(index);
        }
    }

    // Calculates the total price of all items currently in the draft.
    // Inputs: None.
    // Output: BigDecimal - total monetary sum.
    public BigDecimal getTotalPrice() {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderDraftItem item : items) {
            if (item.getPrice() != null) {
                total = total.add(item.getPrice());
            }
        }
        return total;
    }

    // Returns total count of items in the draft.
    public int getItemCount() {
        return items.size();
    }

    // =========================================================================
    // Getters and Setters
    // =========================================================================

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerMobile() {
        return customerMobile;
    }

    public void setCustomerMobile(String customerMobile) {
        this.customerMobile = customerMobile;
    }

    public Garment getCurrentGarment() {
        return currentGarment;
    }

    public void setCurrentGarment(Garment currentGarment) {
        this.currentGarment = currentGarment;
    }

    public OrderDraftItem getCurrentItem() {
        return currentItem;
    }

    public void setCurrentItem(OrderDraftItem currentItem) {
        this.currentItem = currentItem;
    }

    public List<OrderDraftItem> getItems() {
        return items;
    }

    public void setItems(List<OrderDraftItem> items) {
        this.items = (items != null) ? items : new ArrayList<>();
    }

    public Long getExistingOrderId() {
        return existingOrderId;
    }

    public void setExistingOrderId(Long existingOrderId) {
        this.existingOrderId = existingOrderId;
    }
}
