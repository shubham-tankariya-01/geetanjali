package com.geetanjali.backend.service;

import com.geetanjali.backend.dto.form.FieldForm;
import com.geetanjali.backend.dto.form.GarmentForm;
import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.entity.GarmentField;
import com.geetanjali.backend.exception.BusinessRuleException;
import com.geetanjali.backend.exception.ResourceNotFoundException;
import com.geetanjali.backend.repository.GarmentFieldRepository;
import com.geetanjali.backend.repository.GarmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// GarmentService: Handles business logic for garment categories and measurement templates.
// In Express, this would be `garmentService.js` managing garment catalog operations.
// @Service = Tells Spring to instantiate and manage this class as a business service bean.
@Service
// @Transactional(readOnly = true) = Optimizes database query performance since read methods dominate.
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
    // Used on the new order screen and settings screen to render categories.
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

    // Adds a new garment category.
    // Enforces business rules: rejects duplicate names (case-insensitive) and appends to the end of the sort order.
    // If a previously soft-deleted category with the same name exists, reactivates it at the end of the list.
    // Input: form (GarmentForm) - validated form containing garment category name.
    // Output: Garment - saved garment entity.
    // @Transactional = Manages the database write transaction.
    @Transactional
    public Garment addGarment(GarmentForm form) {
        if (form == null || form.getName() == null || form.getName().trim().isBlank()) {
            throw new BusinessRuleException("Garment category name cannot be empty.");
        }

        String trimmedName = form.getName().trim();
        Optional<Garment> existingOpt = garmentRepository.findByNameIgnoreCase(trimmedName);

        if (existingOpt.isPresent()) {
            Garment existing = existingOpt.get();
            if (Boolean.TRUE.equals(existing.getActive())) {
                throw new BusinessRuleException("A garment category named '" + trimmedName + "' already exists.");
            } else {
                // Reactivate previously soft-deleted garment and place it at the end
                existing.setActive(true);
                existing.setSortOrder(calculateNextGarmentSortOrder());
                return garmentRepository.save(existing);
            }
        }

        Garment garment = new Garment(trimmedName, calculateNextGarmentSortOrder(), true);
        return garmentRepository.save(garment);
    }

    // Soft-deletes a garment category by setting active = false.
    // Preserves database integrity so past customer orders and measurements still retain valid foreign keys.
    // Input: garmentId (Long) - garment primary key.
    // Output: None (void).
    // @Transactional = Manages the database write transaction.
    @Transactional
    public void removeGarment(Long garmentId) {
        Garment garment = getGarmentById(garmentId);
        garment.setActive(false);
        garmentRepository.save(garment);
    }

    // Adds a new measurement field to a garment's measurement template.
    // Places the new field at the end of the sort order.
    // Input: garmentId (Long) - garment category ID.
    // Input: form (FieldForm) - field label and unit.
    // Output: GarmentField - saved measurement field entity.
    // @Transactional = Manages the database write transaction.
    @Transactional
    public GarmentField addField(Long garmentId, FieldForm form) {
        Garment garment = getGarmentById(garmentId);

        if (form == null || form.getLabel() == null || form.getLabel().trim().isBlank()) {
            throw new BusinessRuleException("Field label cannot be empty.");
        }

        String trimmedLabel = form.getLabel().trim();
        String unit = (form.getUnit() != null && !form.getUnit().trim().isBlank()) ? form.getUnit().trim() : "inch";

        // Check if field with same label already exists for this garment
        Optional<GarmentField> existingOpt = garmentFieldRepository.findByGarmentIdAndLabelIgnoreCase(garmentId, trimmedLabel);
        if (existingOpt.isPresent()) {
            GarmentField existing = existingOpt.get();
            if (Boolean.TRUE.equals(existing.getActive())) {
                throw new BusinessRuleException("Field '" + trimmedLabel + "' already exists for " + garment.getName() + ".");
            } else {
                // Reactivate previously soft-deleted field and place at the end
                existing.setActive(true);
                existing.setUnit(unit);
                existing.setSortOrder(calculateNextFieldSortOrder(garmentId));
                return garmentFieldRepository.save(existing);
            }
        }

        GarmentField field = new GarmentField(garment, trimmedLabel, unit, calculateNextFieldSortOrder(garmentId), true);
        return garmentFieldRepository.save(field);
    }

    // Renames an existing measurement field and updates its unit.
    // Input: garmentId (Long) - garment category ID.
    // Input: fieldId (Long) - field ID to rename.
    // Input: form (FieldForm) - new label and unit.
    // Output: GarmentField - updated measurement field entity.
    // @Transactional = Manages the database write transaction.
    @Transactional
    public GarmentField renameField(Long garmentId, Long fieldId, FieldForm form) {
        getGarmentById(garmentId);

        GarmentField field = garmentFieldRepository.findByIdAndGarmentId(fieldId, garmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Field not found with id: " + fieldId));

        if (form == null || form.getLabel() == null || form.getLabel().trim().isBlank()) {
            throw new BusinessRuleException("Field label cannot be empty.");
        }

        String trimmedLabel = form.getLabel().trim();
        String unit = (form.getUnit() != null && !form.getUnit().trim().isBlank()) ? form.getUnit().trim() : "inch";

        // Check if another active field already has this label
        Optional<GarmentField> conflictOpt = garmentFieldRepository.findByGarmentIdAndLabelIgnoreCase(garmentId, trimmedLabel);
        if (conflictOpt.isPresent() && !conflictOpt.get().getId().equals(fieldId) && Boolean.TRUE.equals(conflictOpt.get().getActive())) {
            throw new BusinessRuleException("Another field named '" + trimmedLabel + "' already exists for this garment.");
        }

        field.setLabel(trimmedLabel);
        field.setUnit(unit);
        return garmentFieldRepository.save(field);
    }

    // Moves a measurement field UP by swapping sort_order with its immediate upper neighbor.
    // Input: garmentId (Long) - garment category ID.
    // Input: fieldId (Long) - field ID to move up.
    // Output: None (void).
    // @Transactional = Manages the database write transaction.
    @Transactional
    public void moveFieldUp(Long garmentId, Long fieldId) {
        getGarmentById(garmentId);

        List<GarmentField> fields = garmentFieldRepository.findByGarmentIdAndActiveTrueOrderBySortOrderAsc(garmentId);
        int index = -1;
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).getId().equals(fieldId)) {
                index = i;
                break;
            }
        }

        if (index <= 0) {
            // Already at the top or not found; nothing to swap
            return;
        }

        GarmentField current = fields.get(index);
        GarmentField previous = fields.get(index - 1);

        int currentSort = current.getSortOrder();
        int previousSort = previous.getSortOrder();

        // If sort orders happen to be equal, re-index sequentially first
        if (currentSort == previousSort) {
            for (int i = 0; i < fields.size(); i++) {
                fields.get(i).setSortOrder((i + 1) * 10);
            }
            currentSort = current.getSortOrder();
            previousSort = previous.getSortOrder();
        }

        // Swap sort numbers with the neighbour
        current.setSortOrder(previousSort);
        previous.setSortOrder(currentSort);

        garmentFieldRepository.save(current);
        garmentFieldRepository.save(previous);
    }

    // Moves a measurement field DOWN by swapping sort_order with its immediate lower neighbor.
    // Input: garmentId (Long) - garment category ID.
    // Input: fieldId (Long) - field ID to move down.
    // Output: None (void).
    // @Transactional = Manages the database write transaction.
    @Transactional
    public void moveFieldDown(Long garmentId, Long fieldId) {
        getGarmentById(garmentId);

        List<GarmentField> fields = garmentFieldRepository.findByGarmentIdAndActiveTrueOrderBySortOrderAsc(garmentId);
        int index = -1;
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).getId().equals(fieldId)) {
                index = i;
                break;
            }
        }

        if (index == -1 || index >= fields.size() - 1) {
            // Already at the bottom or not found; nothing to swap
            return;
        }

        GarmentField current = fields.get(index);
        GarmentField next = fields.get(index + 1);

        int currentSort = current.getSortOrder();
        int nextSort = next.getSortOrder();

        // If sort orders happen to be equal, re-index sequentially first
        if (currentSort == nextSort) {
            for (int i = 0; i < fields.size(); i++) {
                fields.get(i).setSortOrder((i + 1) * 10);
            }
            currentSort = current.getSortOrder();
            nextSort = next.getSortOrder();
        }

        // Swap sort numbers with the neighbour
        current.setSortOrder(nextSort);
        next.setSortOrder(currentSort);

        garmentFieldRepository.save(current);
        garmentFieldRepository.save(next);
    }

    // Soft-deletes a measurement field by setting active = false.
    // Preserves historical customer measurements stored in the measurement_value table.
    // Input: garmentId (Long) - garment category ID.
    // Input: fieldId (Long) - field ID to soft-delete.
    // Output: None (void).
    // @Transactional = Manages the database write transaction.
    @Transactional
    public void removeField(Long garmentId, Long fieldId) {
        getGarmentById(garmentId);

        GarmentField field = garmentFieldRepository.findByIdAndGarmentId(fieldId, garmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Field not found with id: " + fieldId));

        field.setActive(false);
        garmentFieldRepository.save(field);
    }

    // Calculates the next sort order for a new garment category (places it at the end).
    // Input: None.
    // Output: int - next sort order integer.
    private int calculateNextGarmentSortOrder() {
        List<Garment> activeGarments = garmentRepository.findByActiveTrueOrderBySortOrderAsc();
        int maxOrder = 0;
        for (Garment g : activeGarments) {
            if (g.getSortOrder() != null && g.getSortOrder() > maxOrder) {
                maxOrder = g.getSortOrder();
            }
        }
        return maxOrder + 10;
    }

    // Calculates the next sort order for a new measurement field (places it at the end).
    // Input: garmentId (Long).
    // Output: int - next sort order integer.
    private int calculateNextFieldSortOrder(Long garmentId) {
        List<GarmentField> activeFields = garmentFieldRepository.findByGarmentIdAndActiveTrueOrderBySortOrderAsc(garmentId);
        int maxOrder = 0;
        for (GarmentField f : activeFields) {
            if (f.getSortOrder() != null && f.getSortOrder() > maxOrder) {
                maxOrder = f.getSortOrder();
            }
        }
        return maxOrder + 10;
    }
}
