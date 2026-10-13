package com.geetanjali.backend.controller.advice;

import com.geetanjali.backend.enums.Role;
import com.geetanjali.backend.security.AppUserDetails;
import com.geetanjali.backend.service.ShopService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// GlobalModelAdvice: Injects global shared variables into EVERY Thymeleaf template automatically.
//
// Express / EJS comparison:
// This is exactly like Express app-level middleware:
// `app.use((req, res, next) => { res.locals.shopName = '...'; res.locals.currentUser = req.user; next(); })`.
//
// @ControllerAdvice = Intercepts all controllers across the application.
@ControllerAdvice
public class GlobalModelAdvice {

    private final ShopService shopService;

    // Constructor injection: Spring provides the ShopService bean automatically.
    public GlobalModelAdvice(ShopService shopService) {
        this.shopService = shopService;
    }

    // Exposes the shop name in all Thymeleaf views as ${shopName}.
    // Queries the database only for the shop name via ShopService.
    // Input: None.
    // Output: String - shop display name (e.g. "Geetanjali Tailors").
    //
    // @ModelAttribute = Evaluates this method and adds the return value into the Model for every view template.
    @ModelAttribute("shopName")
    public String shopName() {
        return shopService.getShopName();
    }

    // Exposes the logged-in staff member object in all views as ${currentUser}.
    // Input: userDetails (@AuthenticationPrincipal AppUserDetails) - automatically resolved from the Spring Security session.
    // Output: AppUserDetails - current logged-in user principal, or null if visitor is anonymous.
    //
    // @ModelAttribute = Adds the return value to the model.
    @ModelAttribute("currentUser")
    public AppUserDetails currentUser(@AuthenticationPrincipal AppUserDetails userDetails) {
        return userDetails;
    }

    // Exposes the current user's full name in all views as ${currentUserName}.
    // Input: userDetails (AppUserDetails).
    // Output: String - user's full name, or empty string if anonymous.
    //
    // @ModelAttribute = Adds current user name to the model.
    @ModelAttribute("currentUserName")
    public String currentUserName(@AuthenticationPrincipal AppUserDetails userDetails) {
        return userDetails != null ? userDetails.getFullName() : "";
    }

    // Exposes a boolean flag in all views as ${isOwner}.
    // Allows templates to easily show or hide owner-only features like settings or staff addition.
    // Input: userDetails (AppUserDetails).
    // Output: boolean - true if logged in and role is OWNER, false otherwise.
    //
    // @ModelAttribute = Adds owner boolean flag to the model.
    @ModelAttribute("isOwner")
    public boolean isOwner(@AuthenticationPrincipal AppUserDetails userDetails) {
        return userDetails != null && userDetails.getRole() == Role.OWNER;
    }

    // Exposes the current request URI path in all views as ${currentUri}.
    // Used in navigation bars (layout.html) to highlight the active menu link.
    // Input: request (HttpServletRequest) - the current HTTP request.
    // Output: String - URI path (e.g. "/" or "/customers").
    //
    // @ModelAttribute = Adds current URI path to the model.
    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
