package com.geetanjali.backend.controller;

import com.geetanjali.backend.dto.PendingPaymentsSummary;
import com.geetanjali.backend.service.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// PaymentController: Web MVC controller handling HTTP requests for payments and the pending payments ledger.
//
// What it is: A Spring MVC controller mapping HTTP endpoints under "/payments".
// In Express, this corresponds to `app.get('/payments/pending', ...)`.
//
// Controller layer rule: Controllers are thin; they accept the HTTP request, delegate business logic to ONE service,
// and forward the model attributes to the view template. They NEVER access repositories directly.
//
// @Controller = Tells Spring MVC to register this class as a web controller that renders HTML views.
@Controller
// @RequestMapping("/payments") = Sets the base URL prefix for all endpoints in this controller.
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    // Constructor injection: Spring injects the PaymentService bean.
    // Inputs: paymentService (PaymentService).
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Handles GET /payments/pending: Renders the Pending Payments ledger page.
    // Displays the total shop-wide outstanding money balance at the top, followed by a chronological table
    // of all customer orders that have an unpaid balance (balance > 0).
    //
    // Inputs:
    //   model (Model) - Spring UI model used to pass data attributes to the Thymeleaf template.
    // Output:
    //   String - View path "payments/pending".
    //
    // @GetMapping("/pending") = Maps HTTP GET requests for "/payments/pending" to this method.
    @GetMapping("/pending")
    public String showPendingPayments(Model model) {
        // Delegate calculation of pending rows, due text, and total outstanding to the service layer
        PendingPaymentsSummary summary = paymentService.getPendingPaymentsSummary();

        // Pass summary data to the Thymeleaf view
        model.addAttribute("totalOutstanding", summary.totalOutstanding());
        model.addAttribute("pendingRows", summary.rows());

        return "payments/pending";
    }
}
