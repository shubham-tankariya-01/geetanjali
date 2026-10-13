package com.geetanjali.backend.controller;

import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.service.GarmentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

// MoreController: Web MVC controller handling HTTP requests for the "More" / Settings hub page ("/more").
// Provides navigation links to Garments catalog (Owner only), staff onboarding, and placeholders for Stage 2/3+ modules.
// In Express, this corresponds to `app.get('/more', (req, res) => res.render('more'))`.
// Kept thin: relies on GlobalModelAdvice for current user role, shop name, and URI context.
//
// @Controller = Tells Spring MVC that this class handles HTTP web requests and renders Thymeleaf HTML templates.
@Controller
public class MoreController {

    private final GarmentService garmentService;

    // Constructor injection: Spring provides the GarmentService bean automatically.
    // Input: garmentService (GarmentService).
    public MoreController(GarmentService garmentService) {
        this.garmentService = garmentService;
    }

    // Handles GET /more: Displays the More / Administration overview screen.
    // In Express, this is like `app.get('/more', (req, res) => res.render('more', { garments }))`.
    //
    // Inputs: model (Model) - passes active garments list to Thymeleaf.
    // Output: String - Thymeleaf template path "more".
    //
    // @GetMapping("/more") = Maps HTTP GET requests for "/more" to this handler method.
    @GetMapping("/more")
    public String showMorePage(Model model) {
        List<Garment> garments = garmentService.listActiveGarments();
        model.addAttribute("garments", garments);
        return "more";
    }

    // Handles GET /garments: Redirects to the Owner garment catalog settings page.
    // Inputs: None.
    // Output: String - redirect to "/settings/garments".
    //
    // @GetMapping("/garments") = Maps HTTP GET requests for "/garments" to this handler method.
    @GetMapping("/garments")
    public String showGarments() {
        return "redirect:/settings/garments";
    }

    // Handles GET /settings: Redirects /settings requests to the garment settings management page.
    // Inputs: None.
    // Output: String - redirect string to "/settings/garments".
    //
    // @GetMapping("/settings") = Maps HTTP GET requests for "/settings" to this method.
    @GetMapping("/settings")
    public String redirectToSettings() {
        return "redirect:/settings/garments";
    }
}

