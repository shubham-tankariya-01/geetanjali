package com.geetanjali.backend.security;

import com.geetanjali.backend.entity.AppUser;
import com.geetanjali.backend.entity.Shop;
import com.geetanjali.backend.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;

// AppUserDetails: Wraps our AppUser entity so Spring Security can inspect permissions and credentials.
// In Passport.js / Express, this is the deserialized `req.user` session object.
// Implements UserDetails = Spring Security's standard interface for an authenticated principal.
public class AppUserDetails implements UserDetails {

    // The underlying database entity representing the logged-in staff member.
    private final AppUser appUser;

    // Constructor to wrap an AppUser entity.
    // Input: appUser (AppUser).
    // Output: New AppUserDetails instance.
    public AppUserDetails(AppUser appUser) {
        this.appUser = appUser;
    }

    // Returns granted authorities (roles). Spring Security prefixes roles with "ROLE_".
    // For OWNER -> "ROLE_OWNER", for TAILOR -> "ROLE_TAILOR".
    // Input: None.
    // Output: Collection of GrantedAuthority.
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name())
        );
    }

    // Returns the hashed password used by Spring Security to verify login.
    // Input: None.
    // Output: String (BCrypt hash).
    @Override
    public String getPassword() {
        return appUser.getPasswordHash();
    }

    // Returns the username used for authentication.
    // WHY: Spring Security calls this "getUsername", but in our shop the mobile number is the login identifier.
    // Input: None.
    // Output: String (mobile number).
    @Override
    public String getUsername() {
        return appUser.getMobile();
    }

    // Account status checks: our hackathon app does not lock or expire accounts yet.
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // Helper method to get the user's friendly display name (e.g., "Shubham").
    // Input: None.
    // Output: String.
    public String getFullName() {
        return appUser.getFullName();
    }

    // Helper method to get the user's role.
    // Input: None.
    // Output: Role.
    public Role getRole() {
        return appUser.getRole();
    }

    // Helper method to get the user's tailor shop.
    // Input: None.
    // Output: Shop.
    public Shop getShop() {
        return appUser.getShop();
    }

    // Direct access to the underlying AppUser entity.
    // Input: None.
    // Output: AppUser.
    public AppUser getAppUser() {
        return appUser;
    }

    // Alias for getAppUser().
    public AppUser getUser() {
        return appUser;
    }
}
