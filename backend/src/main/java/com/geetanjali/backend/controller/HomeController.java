package com.geetanjali.backend.controller;

import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.service.OrderService;
import com.geetanjali.backend.service.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;

// HomeController: Handles HTTP requests for the main application dashboard and home landing page ("/").
// Displays a customer search bar, the 20 most recent orders, pending balance summary, and quick-action buttons.
// In Express, this corresponds to `app.get('/', async (req, res) => res.render('home/index', { recentOrders, pendingTotal }))`.
//
// @Controller = Tells Spring MVC that this class handles HTTP web requests and renders views.
@Controller
public class HomeController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    // Constructor injection: Spring automatically injects the OrderService and PaymentService beans.
    // Inputs: orderService (OrderService), paymentService (PaymentService).
    public HomeController(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    // Handles GET /: Renders the home landing page with recent orders, pending payments card, and search interface.
    // Inputs: model (Model) - passes data to the Thymeleaf template (currentUser and shopName are added by GlobalModelAdvice).
    // Output: String - Thymeleaf template path "home/index".
    //
    // @GetMapping("/") = Maps HTTP GET requests for root ("/") to this handler.
    @GetMapping("/")
    public String showHomePage(Model model) {
        // Query the 20 most recent customer orders
        List<CustomerOrder> recentOrders = orderService.getRecentOrders();
        model.addAttribute("recentOrders", recentOrders);

        // Query shop-wide outstanding pending balance
        BigDecimal pendingTotal = paymentService.getTotalPendingBalance();
        model.addAttribute("pendingTotal", pendingTotal);
        model.addAttribute("hasPendingBalance", pendingTotal != null && pendingTotal.compareTo(BigDecimal.ZERO) > 0);

        return "home/index";
    }
}
