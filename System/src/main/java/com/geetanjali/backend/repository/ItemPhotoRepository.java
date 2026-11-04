package com.geetanjali.backend.repository;

import com.geetanjali.backend.entity.ItemPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

// ItemPhotoRepository: Database access interface for item cloth/design photos.
// @Repository = Tells Spring to manage this interface as a data access bean.
@Repository
public interface ItemPhotoRepository extends JpaRepository<ItemPhoto, Long> {

    // Finds all photos associated with a specific order item.
    // Input: orderItemId (Long).
    // Output: List<ItemPhoto>.
    List<ItemPhoto> findByOrderItemId(Long orderItemId);
}
