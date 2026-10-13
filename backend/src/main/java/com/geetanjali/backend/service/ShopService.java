package com.geetanjali.backend.service;

import com.geetanjali.backend.entity.Shop;
import com.geetanjali.backend.repository.ShopRepository;
import org.springframework.stereotype.Service;
import java.util.List;

// ShopService: Handles operations and queries related to the tailor shop profile.
// In Express, this is like your shop helper or shop controller logic.
// @Service = Tells Spring to instantiate and manage this class as a business service bean.
@Service
public class ShopService {

    private final ShopRepository shopRepository;

    // Constructor injection for ShopRepository.
    public ShopService(ShopRepository shopRepository) {
        this.shopRepository = shopRepository;
    }

    // Retrieves the primary shop record for this store deployment.
    // Input: None.
    // Output: Shop entity, or null if no shop has been initialized yet.
    public Shop getPrimaryShop() {
        List<Shop> shops = shopRepository.findAll();
        if (shops.isEmpty()) {
            return null;
        }
        return shops.get(0);
    }

    // Returns the display name of the shop for headers and UI templates.
    // Input: None.
    // Output: String - shop name, or "Geetanjali Tailors" fallback.
    public String getShopName() {
        Shop shop = getPrimaryShop();
        if (shop != null && shop.getName() != null && !shop.getName().isBlank()) {
            return shop.getName();
        }
        return "Geetanjali Tailors";
    }
}
