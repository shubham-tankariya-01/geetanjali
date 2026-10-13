package com.geetanjali.backend.repository;

import com.geetanjali.backend.entity.Garment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

// GarmentRepository: Database access interface for Garment clothing categories (Shirt, Pant, etc.).
// @Repository = Tells Spring to manage this interface as a data access bean.
@Repository
public interface GarmentRepository extends JpaRepository<Garment, Long> {

    // Finds a garment by its name.
    // Input: name (String).
    // Output: Optional<Garment>.
    Optional<Garment> findByName(String name);

    // Finds all active garments ordered by display sort_order.
    // Input: None.
    // Output: List<Garment>.
    List<Garment> findByActiveTrueOrderBySortOrderAsc();
}
