package com.geetanjali.backend.repository;

import com.geetanjali.backend.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

// AppUserRepository: Database access interface for AppUser accounts (staff, owner).
// In Express, this replaces `User.findOne({ mobile })` or `SELECT * FROM app_user WHERE mobile = $1`.
// @Repository = Tells Spring to manage this interface as a data access bean and handle SQL exceptions.
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    // Finds a user by their unique mobile phone number.
    // Spring Data automatically generates "SELECT * FROM app_user WHERE mobile = ?" based on the method name.
    // Input: mobile (String) - the phone number used for login.
    // Output: Optional<AppUser> - contains the user if found, or empty if not found.
    Optional<AppUser> findByMobile(String mobile);

    // Checks if any user with the given mobile number already exists in the database.
    // Input: mobile (String).
    // Output: boolean (true if mobile exists, false otherwise).
    boolean existsByMobile(String mobile);

    // Finds a user by mobile, exact name, or first name
    @Query("SELECT u FROM AppUser u WHERE u.mobile = :input OR LOWER(u.fullName) = LOWER(:input) OR LOWER(u.fullName) LIKE LOWER(CONCAT(:input, ' %'))")
    List<AppUser> findByMobileOrName(@Param("input") String input);
}
