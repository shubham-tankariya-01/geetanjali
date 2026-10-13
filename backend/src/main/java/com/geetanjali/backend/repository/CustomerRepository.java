package com.geetanjali.backend.repository;

import com.geetanjali.backend.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

// CustomerRepository: Database access interface for Customer entities.
// In Express/Mongoose, this is like `Customer.find()`.
// @Repository = Tells Spring to register this interface as a data access bean.
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Finds a customer by mobile number.
    // Input: mobile (String).
    // Output: Optional<Customer>.
    Optional<Customer> findByMobile(String mobile);
}
