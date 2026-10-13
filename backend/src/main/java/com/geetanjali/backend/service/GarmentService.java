package com.geetanjali.backend.service;

import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.entity.GarmentField;
import com.geetanjali.backend.exception.ResourceNotFoundException;
import com.geetanjali.backend.repository.GarmentFieldRepository;
import com.geetanjali.backend.repository.GarmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

// GarmentService: Handles business logic for garment categories and measurement templates.
// In Express, this would be `garmentService.js` managing garment catalog operations.
// @Service = Tells Spring to instantiate and manage this class as a business service bean.
@Service
// @Transactional(readOnly = true) = Optimizes database query performance since this service only reads data.
@Transactional(readOnly = true)
public class GarmentService {

    private final GarmentRepository garmentRepository;
    private final GarmentFieldRepository garmentFieldRepository;

    // Constructor injection: Spring provides the repository beans automatically.
    // Inputs: garmentRepository (GarmentRepository), garmentFieldRepository (GarmentFieldRepository).
    public GarmentService(GarmentRepository garmentRepository, GarmentFieldRepository garmentFieldRepository) {
        this.garmentRepository = garmentRepository;
        this.garmentFieldRepository = garmentFieldRepository;
    }

    // Lists all active garment categories (e.g. Shirt, Pant, Kurta) sorted by their display order.
    // Used on the new order screen to render category selection tiles.
    // Input: None.
    // Output: List<Garment> - active garments sorted by sort_order ascending.
    public List<Garment> listActiveGarments() {
        return garmentRepository.findByActiveTrueOrderBySortOrderAsc();
    }

    // Retrieves a single garment by its primary key ID.
    // Throws ResourceNotFoundException if no garment exists with this ID.
    // Input: id (Long) - garment primary key.
    // Output: Garment - the found garment entity.
    public Garment getGarmentById(Long id) {
        return garmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Garment not found with id: " + id));
    }

    // Retrieves all active measurement fields (e.g. Chest, Waist, Length) for a specific garment.
    // First verifies that the garment exists, then queries the active fields in ascending sort order.
    // Input: garmentId (Long) - garment primary key.
    // Output: List<GarmentField> - active fields ordered by sort_order ascending.
    public List<GarmentField> getActiveFields(Long garmentId) {
        // First verify garment exists; throws ResourceNotFoundException if unknown ID
        getGarmentById(garmentId);

        // Fetch active fields configured for this garment
        return garmentFieldRepository.findByGarmentIdAndActiveTrueOrderBySortOrderAsc(garmentId);
    }
}
