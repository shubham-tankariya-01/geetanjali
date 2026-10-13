package com.geetanjali.backend.dto.draft;

import com.geetanjali.backend.entity.Garment;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// OrderDraftItem: Represents a single custom garment item in the multi-step order wizard cart.
// For example, "1 Shirt with Chest=38.5, Length=30.0, price=₹450, delivery=2026-10-20, pattern photo".
//
// In Express, this corresponds to an item object stored in `req.session.cart.items[i]`.
// Implements Serializable so Spring can safely persist session state across restarts.
public class OrderDraftItem implements Serializable {

    private static final long serialVersionUID = 1L;

    // The selected garment category (e.g. Shirt, Pant, Kurta, Blouse).
    private Garment garment;

    // Numeric measurement values keyed by GarmentField ID (e.g. 101 -> 30.0 for Length, 102 -> 38.0 for Chest).
    private Map<Long, BigDecimal> measurementValues = new LinkedHashMap<>();

    // Special tailor instructions or customer preferences (e.g. "Double stitching, mandarin collar, left pocket").
    private String notes;

    // The promised delivery date for this garment item.
    private LocalDate deliveryDate;

    // Price charged for stitching this garment item.
    // Money is always BigDecimal in Geetanjali.
    private BigDecimal price = BigDecimal.ZERO;

    // List of uploaded cloth/fabric reference photo web URLs (e.g. "/uploads/uuid.jpg").
    private List<String> photoUrls = new ArrayList<>();

    // Zero-argument constructor required for serialization and Java bean conventions.
    public OrderDraftItem() {
    }

    // Convenience constructor initializing an item for a specific garment category.
    // Input: garment (Garment) - chosen garment definition.
    public OrderDraftItem(Garment garment) {
        this.garment = garment;
    }

    // Full constructor.
    // Inputs: garment, measurementValues, notes, deliveryDate, price, photoUrls.
    public OrderDraftItem(Garment garment, Map<Long, BigDecimal> measurementValues, String notes,
                          LocalDate deliveryDate, BigDecimal price, List<String> photoUrls) {
        this.garment = garment;
        if (measurementValues != null) {
            this.measurementValues = new LinkedHashMap<>(measurementValues);
        }
        this.notes = notes;
        this.deliveryDate = deliveryDate;
        this.price = (price != null) ? price : BigDecimal.ZERO;
        if (photoUrls != null) {
            this.photoUrls = new ArrayList<>(photoUrls);
        }
    }

    // Getter for garment.
    public Garment getGarment() {
        return garment;
    }

    // Setter for garment.
    public void setGarment(Garment garment) {
        this.garment = garment;
    }

    // Getter for measurementValues.
    public Map<Long, BigDecimal> getMeasurementValues() {
        return measurementValues;
    }

    // Setter for measurementValues.
    public void setMeasurementValues(Map<Long, BigDecimal> measurementValues) {
        this.measurementValues = (measurementValues != null) ? new LinkedHashMap<>(measurementValues) : new LinkedHashMap<>();
    }

    // Puts a single measurement value into the map.
    public void setMeasurementValue(Long fieldId, BigDecimal value) {
        if (this.measurementValues == null) {
            this.measurementValues = new LinkedHashMap<>();
        }
        if (value != null) {
            this.measurementValues.put(fieldId, value);
        } else {
            this.measurementValues.remove(fieldId);
        }
    }

    // Getter for notes.
    public String getNotes() {
        return notes;
    }

    // Setter for notes.
    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Getter for deliveryDate.
    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    // Setter for deliveryDate.
    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    // Getter for price.
    public BigDecimal getPrice() {
        return price;
    }

    // Setter for price.
    public void setPrice(BigDecimal price) {
        this.price = (price != null) ? price : BigDecimal.ZERO;
    }

    // Getter for photoUrls.
    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    // Setter for photoUrls.
    public void setPhotoUrls(List<String> photoUrls) {
        this.photoUrls = (photoUrls != null) ? new ArrayList<>(photoUrls) : new ArrayList<>();
    }

    // Adds a photo URL to the list.
    public void addPhotoUrl(String photoUrl) {
        if (photoUrl != null && !photoUrl.isBlank()) {
            if (this.photoUrls == null) {
                this.photoUrls = new ArrayList<>();
            }
            this.photoUrls.add(photoUrl);
        }
    }

    // Removes a photo URL from the list.
    public void removePhotoUrl(String photoUrl) {
        if (this.photoUrls != null) {
            this.photoUrls.remove(photoUrl);
        }
    }
}
