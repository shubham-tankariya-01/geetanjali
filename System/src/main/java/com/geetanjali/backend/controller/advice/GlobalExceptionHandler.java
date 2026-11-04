package com.geetanjali.backend.controller.advice;

import com.geetanjali.backend.exception.BusinessRuleException;
import com.geetanjali.backend.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// GlobalExceptionHandler: Intercepts exceptions thrown across all controllers and converts them into appropriate user responses.
// In Express, this corresponds to your global error handling middleware:
// `app.use((err, req, res, next) => { ... })`.
//
// @ControllerAdvice = Tells Spring MVC that this class handles exceptions thrown by any @Controller method in the app.
@ControllerAdvice
public class GlobalExceptionHandler {

    // Handles ResourceNotFoundException when a requested database entity or resource is missing.
    // Renders the friendly 404 error page.
    //
    // Input: ex (ResourceNotFoundException) - the thrown exception containing details of what was not found.
    // Input: model (Model) - passes the error message to the Thymeleaf view.
    // Output: String - Thymeleaf template path "error/404".
    //
    // @ExceptionHandler = Tells Spring to invoke this method whenever a ResourceNotFoundException is thrown.
    // @ResponseStatus = Sets the HTTP response status code to 404 (Not Found).
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/404";
    }

    // Handles BusinessRuleException when business validation fails during an action.
    // Stores the error as a red flash message and redirects the user back to their previous page.
    //
    // WHY Flash Attributes:
    // Flash attributes are temporarily saved in the HTTP session and survive for exactly ONE redirect.
    // When the user lands on the redirected page, the message is displayed and then automatically destroyed,
    // preventing the message from showing again on subsequent page reloads.
    // In Express, this is identical to `req.flash('error', message); res.redirect(referer);`.
    //
    // Input: ex (BusinessRuleException) - contains the failure explanation.
    // Input: request (HttpServletRequest) - used to read the browser's HTTP "Referer" header.
    // Input: redirectAttributes (RedirectAttributes) - carries the flash message across the HTTP redirect.
    // Output: String - redirect instruction to the previous page or home fallback.
    //
    // @ExceptionHandler = Tells Spring to invoke this method whenever a BusinessRuleException is thrown.
    @ExceptionHandler(BusinessRuleException.class)
    public String handleBusinessRule(BusinessRuleException ex,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        // Store red alert banner message for the destination page
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());

        // Read HTTP Referer header to redirect the user back to the exact page they were on
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            return "redirect:" + referer;
        }

        // Safe fallback to home page if Referer is missing (e.g. direct link or bookmark)
        return "redirect:/";
    }
}
