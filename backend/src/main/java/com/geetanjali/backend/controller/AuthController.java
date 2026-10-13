package com.geetanjali.backend.controller;

import com.geetanjali.backend.dto.form.SignupForm;
import com.geetanjali.backend.entity.AppUser;
import com.geetanjali.backend.entity.Shop;
import com.geetanjali.backend.security.AppUserDetails;
import com.geetanjali.backend.service.AuthService;
import com.geetanjali.backend.service.ShopService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// AuthController: Handles HTTP requests for login, logout redirect, and shop/staff signup.
// In Express, this corresponds to `authRouter.get('/login', ...)` and `authRouter.post('/signup', ...)`.
// Notice: This controller is kept thin and delegates all business logic to AuthService and ShopService.
// No repository is ever called here (strict layered architecture).
// @Controller = Tells Spring MVC that this class handles HTTP requests and returns Thymeleaf view templates.
@Controller
public class AuthController {

    private final AuthService authService;
    private final ShopService shopService;

    // Constructor injection for required services.
    public AuthController(AuthService authService, ShopService shopService) {
        this.authService = authService;
        this.shopService = shopService;
    }

    // Handles GET /login: Displays the mobile & password login page.
    // Inputs: authentication (Authentication), model (Model), error/logout query params.
    // Output: String (Thymeleaf template path "auth/login").
    // @GetMapping = Maps HTTP GET requests for "/login" to this method.
    @GetMapping("/login")
    public String showLoginPage(Authentication authentication,
                                @RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                @RequestParam(value = "signupSuccess", required = false) String signupSuccess,
                                Model model) {
        // If no user exists in the database at all, redirect to /signup so owner can complete initial setup
        if (authService.isFirstRun()) {
            return "redirect:/signup";
        }

        // If user is already logged in, redirect them to home page
        if (authentication != null && authentication.isAuthenticated() &&
                !(authentication.getPrincipal() instanceof String && "anonymousUser".equals(authentication.getPrincipal()))) {
            return "redirect:/";
        }

        // Pass flags to template for rendering alert banners
        if (error != null) {
            model.addAttribute("loginError", "Invalid mobile number or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully.");
        }
        if (signupSuccess != null) {
            model.addAttribute("successMessage", "Account created successfully! Please log in with your mobile number.");
        }

        return "auth/login";
    }

    // Handles GET /signup: Displays first-run shop owner setup or staff creation.
    // Accessible to anyone ONLY if no users exist. Once an owner exists, only OWNER may open it.
    // Inputs: authentication (Authentication), model (Model).
    // Output: String (template path "auth/signup" or redirect).
    // @GetMapping = Maps HTTP GET requests for "/signup".
    @GetMapping("/signup")
    public String showSignupPage(Authentication authentication, Model model) {
        // Enforce access rule: first-run public OR authenticated OWNER
        if (!authService.canAccessSignup(authentication)) {
            return "redirect:/login";
        }

        boolean isFirstRun = authService.isFirstRun();
        model.addAttribute("isFirstRun", isFirstRun);

        if (!model.containsAttribute("signupForm")) {
            SignupForm form = new SignupForm();
            if (isFirstRun) {
                form.setShopName(shopService.getShopName());
            }
            model.addAttribute("signupForm", form);
        }

        return "auth/signup";
    }

    // Handles POST /signup: Processes form submission to create Owner or Tailor staff.
    // Inputs: signupForm (@Valid SignupForm), bindingResult (BindingResult), authentication (Authentication), redirectAttributes.
    // Output: String (redirect or re-render "auth/signup" on validation error).
    // @PostMapping = Maps HTTP POST requests for "/signup".
    @PostMapping("/signup")
    public String handleSignup(@Valid @ModelAttribute("signupForm") SignupForm form,
                               BindingResult bindingResult,
                               Authentication authentication,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        // Enforce security guard
        if (!authService.canAccessSignup(authentication)) {
            return "redirect:/login";
        }

        boolean isFirstRun = authService.isFirstRun();
        model.addAttribute("isFirstRun", isFirstRun);

        // Check for duplicate mobile number
        if (form.getMobile() != null && authService.isMobileRegistered(form.getMobile())) {
            bindingResult.rejectValue("mobile", "duplicate", "This mobile number is already registered.");
        }

        // If form has validation errors, re-display the form with error messages
        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }

        // Execute registration logic
        if (isFirstRun) {
            // First run: create the Shop and the primary OWNER
            authService.registerFirstOwner(form);
            redirectAttributes.addFlashAttribute("successMessage", "Shop setup complete! Please log in as the owner.");
            return "redirect:/login?signupSuccess";
        } else {
            // Owner is adding a new staff member (Tailor or Co-owner)
            AppUserDetails currentPrincipal = (AppUserDetails) authentication.getPrincipal();
            Shop shop = currentPrincipal.getShop();
            AppUser createdStaff = authService.registerStaff(form, shop);
            redirectAttributes.addFlashAttribute("successMessage", "Staff member " + createdStaff.getFullName() + " created successfully!");
            return "redirect:/?staffCreated";
        }
    }
}
