package com.geetanjali.backend.controller.api;

import com.geetanjali.backend.entity.Customer;
import com.geetanjali.backend.service.CustomerService;
import com.geetanjali.backend.util.PhoneNumbers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.Optional;

// CustomerApiController: REST controller providing AJAX endpoints for fast customer lookup.
//
// WHY @RestController vs @Controller:
// In Spring MVC, a standard `@Controller` returns a view name string (like "customers/index"),
// which Thymeleaf resolves and renders into full HTML for the browser.
// A `@RestController` combines `@Controller` and `@ResponseBody`. Instead of returning HTML,
// it serializes the returned Java object (CustomerLookupResponse) directly into JSON using Jackson.
// In Express, `@Controller` is `res.render('view')`, while `@RestController` is `res.json(data)`.
//
// @RestController = Tells Spring MVC that all handler methods in this class serialize their return value directly into the HTTP response body as JSON.
@RestController
// @RequestMapping = Sets the common URL base prefix for all endpoints in this controller ("/api/customers").
@RequestMapping("/api/customers")
public class CustomerApiController {

    private final CustomerService customerService;

    // Constructor injection: Spring supplies the CustomerService bean automatically.
    // Input: customerService (CustomerService).
    public CustomerApiController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // Handles GET /api/customers/lookup?mobile=...
    // Lookups a customer by mobile number, normalizing it first.
    // If found, returns { found: true, name, orderCount, lastOrderDate }.
    // If not found, returns { found: false, name: null, orderCount: 0, lastOrderDate: null }.
    //
    // Express comparison: `app.get('/api/customers/lookup', async (req, res) => { ... res.json(result); })`.
    //
    // Input: mobile (String) - query parameter from the URL.
    // Output: CustomerLookupResponse - serialized to JSON by Spring.
    //
    // @GetMapping = Maps HTTP GET requests for "/api/customers/lookup" to this method.
    @GetMapping("/lookup")
    public CustomerLookupResponse lookupCustomer(
            // @RequestParam = Extracts the "mobile" query parameter from the HTTP request URL.
            @RequestParam("mobile") String mobile) {

        // Rule: Always normalize the mobile number first (strip +91, spaces, dashes)
        String normalizedMobile = PhoneNumbers.normalize(mobile);

        // Find customer in database
        Optional<Customer> customerOpt = customerService.findByMobile(normalizedMobile);

        if (customerOpt.isEmpty()) {
            return CustomerLookupResponse.notFound();
        }

        Customer customer = customerOpt.get();
        long orderCount = customerService.countEarlierOrders(customer.getId());
        LocalDate lastOrderDate = customerService.findLastOrderDate(customer.getId()).orElse(null);

        return CustomerLookupResponse.found(customer.getName(), orderCount, lastOrderDate);
    }
}
