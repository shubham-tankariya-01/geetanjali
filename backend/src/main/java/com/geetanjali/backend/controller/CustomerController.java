package com.geetanjali.backend.controller;

import com.geetanjali.backend.dto.response.GarmentLatestMeasurement;
import com.geetanjali.backend.entity.Customer;
import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.service.CustomerService;
import com.geetanjali.backend.service.MeasurementService;
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
// Kept intentionally thin: calls only service methods, never touches repositories directly.
//
// @Controller = Tells Spring MVC that this class handles HTTP web requests and renders Thymeleaf HTML templates.
@Controller
// @RequestMapping = Sets the base URL path for all endpoints in this controller ("/customers").
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final MeasurementService measurementService;

    // Constructor injection: Spring provides the service beans automatically.
    // Inputs: customerService (CustomerService), measurementService (MeasurementService).
    public CustomerController(CustomerService customerService, MeasurementService measurementService) {
        this.customerService = customerService;
        this.measurementService = measurementService;
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

    // Handles GET /customers/{id}: Displays customer detail page showing name, phone, order summary,
    // and the latest measurements recorded for each active garment (Shirt, Pant, Kurta, Blouse).
    // Throws ResourceNotFoundException if customer with this ID is missing, automatically triggering the 404 page.
    //
    // Express comparison: `app.get('/customers/:id', (req, res) => res.render('customers/detail', { customer, garmentMeasurements }))`.
    //
    // Input: id (@PathVariable("id") Long) - customer primary key from URL.
    // Input: model (Model) - passes customer details, order summary, and measurements to Thymeleaf.
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

        // Fetch customer's full order history (newest first)
        List<CustomerOrder> orders = customerService.getCustomerOrders(id);

        // Job 3: Fetch newest measurement snapshot per active garment for this customer
        List<GarmentLatestMeasurement> garmentMeasurements = measurementService.getLatestPerGarment(customer);

        model.addAttribute("customer", customer);
        model.addAttribute("orders", orders);
        model.addAttribute("orderCount", orderCount);
        model.addAttribute("lastOrderDate", lastOrderDate);
        model.addAttribute("garmentMeasurements", garmentMeasurements);

        return "customers/detail";
    }
}
