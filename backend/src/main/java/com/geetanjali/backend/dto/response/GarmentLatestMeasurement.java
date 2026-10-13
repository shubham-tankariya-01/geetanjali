package com.geetanjali.backend.dto.response;

import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.entity.Measurement;
import com.geetanjali.backend.entity.MeasurementValue;
import java.util.List;

// GarmentLatestMeasurement: DTO grouping an active garment with its most recent measurement snapshot for a customer.
// In Express/EJS, this is like `{ garment, measurement, values }` rendered in a card on the customer profile page.
// On the customer detail page (/customers/{id}), the app loops through all active garments (Shirt, Pant, Kurta, Blouse)
// and displays each category's latest recorded measurements or a "No measurements yet" indicator.
//
// Records in Java 16+ are immutable data carriers with concise syntax.
public record GarmentLatestMeasurement(
        // The active garment definition (e.g. Shirt, Pant).
        Garment garment,

        // The newest Measurement snapshot entity for this customer and garment, or null if never measured.
        Measurement measurement,

        // The list of MeasurementValue entities for this snapshot (Chest, Waist, etc.), or empty list if none.
        List<MeasurementValue> values
) {

    // Helper method to check if a measurement snapshot exists for this garment.
    // Inputs: None.
    // Output: boolean - true if a measurement snapshot exists, false otherwise.
    public boolean hasMeasurement() {
        return measurement != null;
    }
}
