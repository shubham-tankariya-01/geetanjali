package com.geetanjali.backend.service;

import com.geetanjali.backend.dto.draft.OrderDraft;
import com.geetanjali.backend.dto.draft.OrderDraftItem;
import com.geetanjali.backend.dto.form.CustomerStepForm;
import com.geetanjali.backend.entity.Customer;
import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.exception.ResourceNotFoundException;
import com.geetanjali.backend.repository.CustomerRepository;
import com.geetanjali.backend.repository.GarmentRepository;
import com.geetanjali.backend.util.PhoneNumbers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.geetanjali.backend.dto.form.ItemForm;
import java.util.Optional;

// OrderWizardService: Coordinates the multi-step order placement wizard and manages the session draft.
// In Express, this corresponds to helper business functions manipulating `req.session.orderDraft`.
//
// @Service = Tells Spring to instantiate and manage this class as a business service bean.
@Service
// @Transactional(readOnly = true) = Default read-only transactions since wizard steps mainly read and mutate in-memory session.
@Transactional(readOnly = true)
public class OrderWizardService {

    private final OrderDraft orderDraft;
    private final CustomerRepository customerRepository;
    private final GarmentRepository garmentRepository;

    // Constructor injection: Spring provides the session-scoped OrderDraft proxy and repository beans.
    // Inputs: orderDraft (OrderDraft), customerRepository (CustomerRepository), garmentRepository (GarmentRepository).
    public OrderWizardService(OrderDraft orderDraft,
                              CustomerRepository customerRepository,
                              GarmentRepository garmentRepository) {
        this.orderDraft = orderDraft;
        this.customerRepository = customerRepository;
        this.garmentRepository = garmentRepository;
    }

    // Returns the active session-scoped order draft bean.
    // Inputs: None.
    // Output: OrderDraft - active shopping cart draft.
    public OrderDraft getDraft() {
        return orderDraft;
    }

    // Starts a fresh order wizard session: resets the draft and clears all previous state.
    // Used by GET /orders/new.
    // Inputs: None.
    // Output: None (void).
    public void startFresh() {
        orderDraft.reset();
    }

    // Starts the order wizard pre-populated for a specific existing customer.
    // For example, when a tailor clicks "Create New Order" from a customer profile page (/customers/42).
    // Inputs: customerId (Long) - database ID of the chosen customer.
    // Output: None (void).
    public void startForCustomer(Long customerId) {
        orderDraft.reset();
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        orderDraft.setCustomer(customer.getId(), customer.getName(), customer.getMobile());
    }

    // Starts the wizard to append new garment items to an existing open order.
    // Pre-populates the customer in the session draft and sets existingOrderId, allowing Step 1 to be skipped!
    // Input: order (CustomerOrder) - existing order to append items to.
    public void startAppendToOrder(CustomerOrder order) {
        orderDraft.reset();
        Customer customer = order.getCustomer();
        orderDraft.setCustomer(customer.getId(), customer.getName(), customer.getMobile());
        orderDraft.setExistingOrderId(order.getId());
    }

    // Step 1: Sets the chosen customer on the order draft from the submitted form.
    // Normalizes the mobile number and checks if the customer already exists in PostgreSQL.
    // If existing, associates their primary key ID. If new, stores the name and mobile for creation upon final order placement.
    //
    // Input: form (CustomerStepForm) - validated form data from wizard step 1.
    // Output: None (void).
    public void setCustomer(CustomerStepForm form) {
        String normalizedMobile = PhoneNumbers.normalize(form.getMobile());

        // Check if a customer with this mobile exists in the database
        Optional<Customer> existingOpt = customerRepository.findByMobile(normalizedMobile);

        if (existingOpt.isPresent()) {
            Customer existing = existingOpt.get();
            // Link to the existing customer
            orderDraft.setCustomer(existing.getId(), existing.getName(), existing.getMobile());
        } else {
            // New customer to be created when the order is committed
            orderDraft.setCustomer(null, form.getName().trim(), normalizedMobile);
        }

        // Record optional existing order ID if adding to an open order
        orderDraft.setExistingOrderId(form.getExistingOrderId());
    }

    // Step 2: Selects a garment category for the item currently being added to the draft.
    // Verifies that the garment exists in the database, sets it as current on the draft,
    // and initializes the working OrderDraftItem.
    //
    // Input: garmentId (Long) - chosen garment primary key (e.g. 1 for Shirt).
    // Output: Garment - the loaded garment entity.
    public Garment selectGarment(Long garmentId) {
        Garment garment = garmentRepository.findById(garmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Garment not found with id: " + garmentId));

        orderDraft.setCurrentGarment(garment);
        orderDraft.setCurrentItem(new OrderDraftItem(garment));

        return garment;
    }

    // Step 3: Copies validated form inputs into an OrderDraftItem and appends it to the cart.
    // Clears the current garment and working item so that the draft is ready for the next garment or final summary.
    //
    // Input: form (ItemForm) - validated item form.
    // Output: OrderDraftItem - the created draft item.
    public OrderDraftItem saveItem(ItemForm form) {
        Garment garment = orderDraft.getCurrentGarment();
        if (garment == null) {
            throw new IllegalStateException("Cannot save item without a selected garment");
        }

        OrderDraftItem item = new OrderDraftItem();
        item.setGarment(garment);
        item.setMeasurementValues(form.getMeasurements());
        item.setNotes(form.getNotes());
        item.setDeliveryDate(form.getDeliveryDate());
        item.setPrice(form.getPrice());
        item.setPhotoUrls(form.getPhotoUrls());
        item.setPhotoPublicIds(form.getPhotoPublicIds());

        // Add to the draft items list
        orderDraft.addItem(item);

        // Reset current working garment for next item
        orderDraft.setCurrentGarment(null);
        orderDraft.setCurrentItem(null);

        return item;
    }

    // Updates an existing draft item at the specified 0-based position index.
    // Inputs: index (int), form (ItemForm).
    // Output: OrderDraftItem - the updated draft item.
    public OrderDraftItem updateItem(int index, ItemForm form) {
        if (index < 0 || index >= orderDraft.getItemCount()) {
            throw new IndexOutOfBoundsException("Invalid item index: " + index);
        }

        OrderDraftItem item = orderDraft.getItems().get(index);
        item.setMeasurementValues(form.getMeasurements());
        item.setNotes(form.getNotes());
        item.setDeliveryDate(form.getDeliveryDate());
        item.setPrice(form.getPrice());
        item.setPhotoUrls(form.getPhotoUrls());
        item.setPhotoPublicIds(form.getPhotoPublicIds());

        // Clear working garment
        orderDraft.setCurrentGarment(null);
        orderDraft.setCurrentItem(null);

        return item;
    }

    // Removes an item from the draft and deletes its uploaded photo files from storage.
    // Inputs: index (int), photoStorageService (PhotoStorageService).
    // Output: None (void).
    public void removeItemAndPhotos(int index, com.geetanjali.backend.service.storage.PhotoStorageService photoStorageService) {
        if (index < 0 || index >= orderDraft.getItemCount()) {
            return;
        }

        OrderDraftItem item = orderDraft.getItems().get(index);

        // Delete photo files from disk if storage service provided
        if (photoStorageService != null && item.getPhotoPublicIds() != null) {
            for (String publicId : item.getPhotoPublicIds()) {
                photoStorageService.delete(publicId);
            }
        }

        orderDraft.removeItem(index);
    }
}
