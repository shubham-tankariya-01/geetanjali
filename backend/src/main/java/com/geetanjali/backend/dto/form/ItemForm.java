package com.geetanjali.backend.dto.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// ItemForm: Form-backing object (DTO) for Wizard Step 3: Item Details & Measurements.
// In Express, this corresponds to `req.body` containing measurement inputs, notes, delivery date, and price.
//
// WHY a form class instead of an Entity:
// 1. Decouples HTTP request representation from PostgreSQL database tables.
// 2. An item spans multiple entities: measurement values (measurement_value), item instructions & delivery (order_item),
//    and wizard action buttons (action="more" vs action="finish"). No single entity can represent this combined state.
// 3. Protects against Mass Assignment / Over-Posting attacks by exposing only form-relevant fields.
public class ItemForm {

    // Map of measurement numbers keyed by GarmentField ID (e.g. 101 -> 30.5).
    // Spring MVC automatically binds form inputs named `measurements[101]` into this map.
    // Blank boxes produce null or absent entries, which are fully allowed in tailor shops.
    private Map<Long, BigDecimal> measurements = new LinkedHashMap<>();

    // Custom tailor styling instructions (e.g. "Slim fit, mandarin collar, extra lining").
    private String notes;

    // Promised delivery date for this garment item.
    //
    // @NotNull = Enforces that the tailor must select a delivery date.
    @NotNull(message = "Promised delivery date is required")
    private LocalDate deliveryDate;

    // Stitching price for this garment item.
    // Money is always BigDecimal in Geetanjali.
    //
    // @NotNull = Enforces that price must be provided.
    @NotNull(message = "Stitching price is required")
    // @DecimalMin = Ensures price cannot be negative.
    @DecimalMin(value = "0.00", message = "Price cannot be negative")
    private BigDecimal price = BigDecimal.ZERO;

    // Action button clicked: "more" (Save item & add more) or "finish" (Save item & finish).
    private String action = "finish";

    // List of uploaded cloth/pattern photo URLs (e.g. "/uploads/uuid.jpg").
    private List<String> photoUrls = new ArrayList<>();

    // List of uploaded cloth/pattern photo public IDs (e.g. "uuid.jpg").
    private List<String> photoPublicIds = new ArrayList<>();

    // Default zero-argument constructor required by Spring form binding.
    public ItemForm() {
    }

    // Convenience constructor.
    public ItemForm(LocalDate deliveryDate, BigDecimal price) {
        this.deliveryDate = deliveryDate;
        this.price = (price != null) ? price : BigDecimal.ZERO;
    }

    // =========================================================================
    // Getters and Setters
    // =========================================================================

    public Map<Long, BigDecimal> getMeasurements() {
        return measurements;
    }

    public void setMeasurements(Map<Long, BigDecimal> measurements) {
        this.measurements = (measurements != null) ? new LinkedHashMap<>(measurements) : new LinkedHashMap<>();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = (price != null) ? price : BigDecimal.ZERO;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    public void setPhotoUrls(List<String> photoUrls) {
        this.photoUrls = (photoUrls != null) ? new ArrayList<>(photoUrls) : new ArrayList<>();
    }

    public List<String> getPhotoPublicIds() {
        return photoPublicIds;
    }

    public void setPhotoPublicIds(List<String> photoPublicIds) {
        this.photoPublicIds = (photoPublicIds != null) ? new ArrayList<>(photoPublicIds) : new ArrayList<>();
    }
}
