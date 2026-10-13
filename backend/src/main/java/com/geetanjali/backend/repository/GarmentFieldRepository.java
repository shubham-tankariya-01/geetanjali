package com.geetanjali.backend.repository;

import com.geetanjali.backend.entity.GarmentField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

// GarmentFieldRepository: Database access interface for dynamic garment measurement fields.
// @Repository = Tells Spring to manage this interface as a data access bean.
@Repository
public interface GarmentFieldRepository extends JpaRepository<GarmentField, Long> {

    // Finds all active measurement fields for a given garment, ordered by sort order.
    // Input: garmentId (Long).
    // Output: List<GarmentField>.
    List<GarmentField> findByGarmentIdAndActiveTrueOrderBySortOrderAsc(Long garmentId);
}
