package com.geetanjali.backend.security;

import com.geetanjali.backend.entity.AppUser;
import com.geetanjali.backend.repository.AppUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// AppUserDetailsService: Service used by Spring Security to look up a user by their credentials.
// In Passport.js LocalStrategy, this is equivalent to the `findOne({ username })` verification callback.
// @Service = Tells Spring to instantiate and manage this class as a business service bean.
@Service
public class AppUserDetailsService implements UserDetailsService {

    // Repository used to query the app_user table in PostgreSQL.
    private final AppUserRepository appUserRepository;

    // Constructor injection: Spring provides the AppUserRepository dependency automatically.
    // Input: appUserRepository (AppUserRepository).
    public AppUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    // Called automatically by Spring Security during form login submission.
    // Input: mobile (String) - entered into the mobile field on the login page.
    // Output: UserDetails - populated AppUserDetails principal object.
    // Throws: UsernameNotFoundException if no user with this mobile number exists.
    @Override
    public UserDetails loadUserByUsername(String mobile) throws UsernameNotFoundException {
        // Normalize mobile number (strips spaces, dashes, +91 prefix)
        String normalizedMobile = com.geetanjali.backend.util.PhoneNumbers.normalize(mobile);

        // Query database for user with matching mobile number
        AppUser user = appUserRepository.findByMobile(normalizedMobile)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with mobile: " + mobile));

        // Wrap entity in our custom UserDetails adapter
        return new AppUserDetails(user);
    }
}
