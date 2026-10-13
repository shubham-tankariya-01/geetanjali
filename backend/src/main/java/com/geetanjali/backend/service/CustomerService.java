package com.geetanjali.backend.service;

import com.geetanjali.backend.entity.Customer;
import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.exception.ResourceNotFoundException;
import com.geetanjali.backend.repository.CustomerOrderRepository;
import com.geetanjali.backend.repository.CustomerRepository;
import com.geetanjali.backend.util.PhoneNumbers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// CustomerService: Encapsulates all read operations and business rules for customer profiles and order counts.
// In Express, this corresponds to `customerService.js`.
// @Service = Tells Spring to instantiate and manage this class as a business service bean.
@Service
// @Transactional(readOnly = true) = Tells Spring that methods in this service only read from the database, enabling Hibernate read optimizations.
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final Clock clock;

    // Constructor injection: Spring provides repository beans and the Asia/Kolkata Clock bean.
    // Inputs: customerRepository (CustomerRepository), customerOrderRepository (CustomerOrderRepository), clock (Clock).
    public CustomerService(CustomerRepository customerRepository,
                           CustomerOrderRepository customerOrderRepository,
                           Clock clock) {
        this.customerRepository = customerRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.clock = clock;
    }

    // Searches customers by name (case-insensitive) or phone number.
    // If the search term is empty or blank, returns all customers ordered alphabetically by name.
    // In Express, this is like `Customer.find({ $or: [{ name: /q/i }, { mobile: /q/ }] }).sort('name')`.
    // Input: query (String) - search keyword (name or phone digits).
    // Output: List<Customer> - matching customers list.
    public List<Customer> search(String query) {
        if (query == null || query.isBlank()) {
            return customerRepository.findAllByOrderByNameAsc();
        }

        // Clean the search query
        String cleaned = query.trim();

        // Pass as a bound parameter into the repository query (prevents SQL injection)
        return customerRepository.searchByNameOrMobile(cleaned);
    }

    // Finds a customer by their 10-digit mobile number.
    // Normalizes input first (e.g. strips "+91", dashes, spaces).
    // Input: mobile (String) - raw phone number.
    // Output: Optional<Customer> - wrapped customer or empty if not found.
    public Optional<Customer> findByMobile(String mobile) {
        // Strip dashes, spaces, and country code into clean 10 digits
        String normalized = PhoneNumbers.normalize(mobile);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        return customerRepository.findByMobile(normalized);
    }

    // Retrieves a single customer by primary key ID.
    // Throws ResourceNotFoundException if the customer does not exist.
    // Input: id (Long) - customer primary key.
    // Output: Customer - found customer entity.
    public Customer getById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    // Counts the number of earlier orders placed by a customer.
    // Input: customerId (Long).
    // Output: long - total count of customer orders.
    public long countEarlierOrders(Long customerId) {
        return customerOrderRepository.countByCustomerId(customerId);
    }

    // Finds the date of the customer's most recent order.
    // WHY Clock: The order timestamp is stored as UTC Instant in PostgreSQL.
    // We convert it to local Indian date using the injected Clock (Asia/Kolkata).
    // Input: customerId (Long).
    // Output: Optional<LocalDate> - latest order date, or empty if customer has never ordered.
    public Optional<LocalDate> findLastOrderDate(Long customerId) {
        Optional<CustomerOrder> latestOrder = customerOrderRepository.findFirstByCustomerIdOrderByCreatedAtDesc(customerId);

        if (latestOrder.isEmpty()) {
            return Optional.empty();
        }

        // Convert UTC Instant into local Indian Standard Time (IST) date
        LocalDate localOrderDate = latestOrder.get()
                .getCreatedAt()
                .atZone(clock.getZone())
                .toLocalDate();

        return Optional.of(localOrderDate);
    }
}
