package com.geetanjali.backend.controller;

import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

// HomeController: Handles HTTP requests for the main application dashboard and home landing page ("/").
// Displays a customer search bar, the 20 most recent orders, and floating quick-action buttons.
// In Express, this corresponds to `app.get('/', async (req, res) => res.render('home/index', { recentOrders }))`.
//
// @Controller = Tells Spring MVC that this class handles HTTP web requests and renders views.
@Controller
public class HomeController {

    private final OrderService orderService;

    // Constructor injection: Spring automatically injects the OrderService bean.
    // Input: orderService (OrderService).
    public HomeController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Handles GET /: Renders the home landing page with recent orders and search interface.
    // Inputs: model (Model) - passes data to the Thymeleaf template (currentUser and shopName are added by GlobalModelAdvice).
    // Output: String - Thymeleaf template path "home/index".
    //
    // @GetMapping("/") = Maps HTTP GET requests for root ("/") to this handler.
    @GetMapping("/")
    public String showHomePage(Model model) {
        // Query the 20 most recent customer orders
        List<CustomerOrder> recentOrders = orderService.getRecentOrders();
        model.addAttribute("recentOrders", recentOrders);

        return "home/index";
    }
}
