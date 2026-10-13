package com.geetanjali.backend.controller;

import com.geetanjali.backend.entity.Customer;
import com.geetanjali.backend.service.CustomerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
import java.util.List;

// CustomerController: Web MVC controller handling HTTP requests for the customer directory and customer profiles.
// In Express, this corresponds to `customerRouter.get('/customers', ...)` and `customerRouter.get('/customers/:id', ...)`.
// Kept intentionally thin: calls only CustomerService, never touches repositories directly.
//
// @Controller = Tells Spring MVC that this class handles HTTP web requests and renders Thymeleaf HTML templates.
@Controller
// @RequestMapping = Sets the base URL path for all endpoints in this controller ("/customers").
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    // Constructor injection: Spring provides the CustomerService bean automatically.
    // Input: customerService (CustomerService).
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // Handles GET /customers?q=...: Displays the customer directory list with a search box.
    // If "q" is provided, filters customers by name or phone number. If "q" is omitted, lists all customers.
    //
    // Express comparison: `app.get('/customers', (req, res) => res.render('customers/index', { customers, q }))`.
    //
    // Input: query (@RequestParam(value="q", required=false) String) - optional search term.
    // Input: model (Model) - passes data to the Thymeleaf template.
    // Output: String - Thymeleaf template path "customers/index".
    //
    // @GetMapping = Maps HTTP GET requests for "/customers" to this handler method.
    @GetMapping
    public String listCustomers(
            // @RequestParam = Binds the optional "q" query parameter from the URL.
            @RequestParam(value = "q", required = false) String query,
            Model model) {

        // Query customers via service
        List<Customer> customers = customerService.search(query);

        // Add attributes to view model
        model.addAttribute("customers", customers);
        model.addAttribute("q", query != null ? query.trim() : "");

        return "customers/index";
    }

    // Handles GET /customers/{id}: Displays a basic customer detail page showing name, phone, and order summary.
    // Throws ResourceNotFoundException if customer with this ID is missing, automatically triggering the 404 page.
    //
    // Express comparison: `app.get('/customers/:id', (req, res) => res.render('customers/detail', { customer }))`.
    //
    // Input: id (@PathVariable("id") Long) - customer primary key from URL.
    // Input: model (Model) - passes customer details and order summary to Thymeleaf.
    // Output: String - Thymeleaf template path "customers/detail".
    //
    // @GetMapping = Maps HTTP GET requests for "/customers/{id}" to this method.
    @GetMapping("/{id}")
    public String showCustomerDetail(
            // @PathVariable = Extracts the "{id}" placeholder from the URL path.
            @PathVariable("id") Long id,
            Model model) {

        // Service throws ResourceNotFoundException if id does not exist
        Customer customer = customerService.getById(id);
        long orderCount = customerService.countEarlierOrders(id);
        LocalDate lastOrderDate = customerService.findLastOrderDate(id).orElse(null);

        model.addAttribute("customer", customer);
        model.addAttribute("orderCount", orderCount);
        model.addAttribute("lastOrderDate", lastOrderDate);

        return "customers/detail";
    }
}
