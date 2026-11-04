package com.geetanjali.backend.repository;

import com.geetanjali.backend.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

// CustomerRepository: Database access interface for Customer entities.
// In Express/Mongoose, this is like `Customer.find()`.
// @Repository = Tells Spring to register this interface as a data access bean.
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Finds a customer by mobile number.
    // In Express, this is like `Customer.findOne({ mobile })`.
    // Input: mobile (String).
    // Output: Optional<Customer>.
    Optional<Customer> findByMobile(String mobile);

    // Finds all customers ordered alphabetically by name for the default customer list.
    // Derived query: Spring Data generates `SELECT * FROM customer ORDER BY name ASC`.
    // Input: None.
    // Output: List<Customer>.
    List<Customer> findAllByOrderByNameAsc();

    // Custom query to search customers by name (case-insensitive) or by mobile number.
    // WHY Custom Query vs Derived Query:
    // A derived query with multiple OR conditions like `findByNameContainingIgnoreCaseOrMobileContaining`
    // creates a very long method name and requires passing the same search term twice.
    // A custom @Query with a single named bound parameter (:query) is much cleaner and safer against SQL injection.
    //
    // Input: query (String) - search keyword.
    // Output: List<Customer> - matching customers ordered by name.
    @Query("SELECT c FROM Customer c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR c.mobile LIKE CONCAT('%', :query, '%') ORDER BY c.name ASC")
    List<Customer> searchByNameOrMobile(@Param("query") String query);
}
