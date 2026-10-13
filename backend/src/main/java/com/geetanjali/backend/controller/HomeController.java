package com.geetanjali.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// HomeController: Handles HTTP requests for the main application dashboard and home landing page ("/").
// In Express, this corresponds to `app.get('/', (req, res) => res.render('home/index'))`.
// Kept intentionally thin: templates receive logged-in user details and shop name via GlobalControllerAdvice.
// @Controller = Tells Spring MVC that this class handles HTTP web requests and renders views.
@Controller
public class HomeController {

    // Handles GET /: Renders the home landing page.
    // Input: None (currentUser is injected globally into model).
    // Output: String (Thymeleaf template path "home/index").
    // @GetMapping = Maps HTTP GET requests for "/" to this handler.
    @GetMapping("/")
    public String showHomePage() {
        return "home/index";
    }
}
