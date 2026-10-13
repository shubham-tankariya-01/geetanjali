package com.geetanjali.backend.service;

import com.geetanjali.backend.dto.form.SignupForm;
import com.geetanjali.backend.entity.AppUser;
import com.geetanjali.backend.entity.Shop;
import com.geetanjali.backend.enums.Role;
import com.geetanjali.backend.repository.AppUserRepository;
import com.geetanjali.backend.repository.ShopRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

// AuthService: Encapsulates all business logic for registration, authentication checks, and user creation.
// In Express, this would be your `authService.js` or `authController` helper functions.
// @Service = Tells Spring to create one shared singleton instance of this class and manage it.
@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;

    // Constructor injection: Spring automatically injects the required repository and encoder beans.
    public AuthService(AppUserRepository appUserRepository,
                       ShopRepository shopRepository,
                       PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.shopRepository = shopRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Checks if the database has zero users (i.e. fresh application installation).
    // Input: None.
    // Output: boolean (true if no users exist in database yet).
    public boolean isFirstRun() {
        return appUserRepository.count() == 0;
    }

    // Checks if a given mobile number is already taken by another user.
    // Input: mobile (String).
    // Output: boolean (true if mobile is registered).
    public boolean isMobileRegistered(String mobile) {
        return appUserRepository.existsByMobile(mobile);
    }

    // Checks whether the current visitor is allowed to view or submit the /signup page.
    // Rule: Open to anyone on first-run (when no users exist). After that, only an authenticated OWNER can open it.
    // Input: auth (Authentication object from Spring Security).
    // Output: boolean (true if allowed, false if forbidden).
    public boolean canAccessSignup(Authentication auth) {
        // If no user exists yet in the database, allow public access for initial setup
        if (isFirstRun()) {
            return true;
        }

        // If users already exist, visitor must be logged in
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        // Check if the logged-in user possesses the OWNER role using a simple loop (plain boring Java)
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if ("ROLE_OWNER".equals(authority.getAuthority())) {
                return true;
            }
        }

        return false;
    }

    // Registers the first shop owner and creates the shop entity during first-run setup.
    // Input: form (SignupForm containing shopName, fullName, mobile, password).
    // Output: The newly saved AppUser entity.
    // @Transactional = Runs all database operations in a single atomic transaction; rolls back if an error occurs.
    @Transactional
    public AppUser registerFirstOwner(SignupForm form) {
        // Find existing shop or create a new one
        Shop shop;
        List<Shop> existingShops = shopRepository.findAll();
        if (existingShops.isEmpty()) {
            String name = (form.getShopName() != null && !form.getShopName().trim().isEmpty())
                    ? form.getShopName().trim()
                    : "Geetanjali Tailors";
            shop = new Shop(name);
            shop = shopRepository.save(shop);
        } else {
            shop = existingShops.get(0);
        }

        // Hash the plain text password using BCrypt
        String encodedPassword = passwordEncoder.encode(form.getPassword());

        // Create the owner user
        AppUser owner = new AppUser(
                shop,
                form.getFullName().trim(),
                form.getMobile().trim(),
                encodedPassword,
                Role.OWNER
        );

        return appUserRepository.save(owner);
    }

    // Registers a new staff member (e.g. tailor or assistant owner) under an existing shop.
    // Input: form (SignupForm), currentShop (Shop).
    // Output: The newly saved AppUser entity.
    // @Transactional = Ensures atomic database insertion.
    @Transactional
    public AppUser registerStaff(SignupForm form, Shop currentShop) {
        // Determine role: use selected role or default to TAILOR
        Role role = (form.getRole() != null) ? form.getRole() : Role.TAILOR;

        // Hash password
        String encodedPassword = passwordEncoder.encode(form.getPassword());

        AppUser staff = new AppUser(
                currentShop,
                form.getFullName().trim(),
                form.getMobile().trim(),
                encodedPassword,
                role
        );

        return appUserRepository.save(staff);
    }
}
