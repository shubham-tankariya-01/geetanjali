package com.geetanjali.backend.controller;

import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

// OrderController: Web MVC controller handling HTTP requests for viewing customer orders.
// In Express, this corresponds to `app.get('/orders/:id', (req, res) => res.render('orders/detail', { order }))`.
//
// CONTROLLER THINNESS RULE:
// Controllers only read the request, validate/extract parameters, invoke ONE service method, and select the view.
// They never contain business calculations or query database repositories directly.
//
// @Controller = Tells Spring MVC that this class handles web requests and returns Thymeleaf template view names.
@Controller
// @RequestMapping = Sets the base URL path for this controller to "/orders".
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    // Constructor injection: Spring automatically injects the OrderService bean.
    // Input: orderService (OrderService).
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Handles GET /orders/{id}: Displays the complete order details page.
    // Shows the customer info header, financial summary, and item cards with photos and measurements.
    // If the order does not exist, orderService throws ResourceNotFoundException which GlobalExceptionHandler maps to HTTP 404.
    //
    // Express comparison: `app.get('/orders/:id', async (req, res) => { const order = await orderService.get(req.params.id); res.render('orders/detail', { order }); });`
    //
    // Inputs:
    //   id (@PathVariable("id") Long) - primary key ID of the CustomerOrder.
    //   model (Model) - passes view attributes to the Thymeleaf template.
    // Output: String - Thymeleaf template path "orders/detail".
    //
    // @GetMapping("/{id}") = Maps HTTP GET requests for "/orders/{id}" to this method.
    @GetMapping("/{id}")
    public String showOrderDetail(@PathVariable("id") Long id, Model model) {
        // Fetch order through service layer. Throws ResourceNotFoundException (404) if missing.
        CustomerOrder order = orderService.getOrderOrThrow(id);

        // Pass entity directly to template (open-in-view allows on-demand lazy loading of items, photos, and measurements)
        model.addAttribute("order", order);

        return "orders/detail";
    }
}
