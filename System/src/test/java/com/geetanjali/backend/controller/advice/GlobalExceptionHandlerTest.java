package com.geetanjali.backend.controller.advice;

import com.geetanjali.backend.exception.BusinessRuleException;
import com.geetanjali.backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Customer #99 not found");
        Model model = new ConcurrentModel();

        String view = exceptionHandler.handleResourceNotFound(ex, model);

        assertEquals("error/404", view);
        assertEquals("Customer #99 not found", model.getAttribute("errorMessage"));
    }

    @Test
    void testHandleBusinessRuleWithReferer() {
        BusinessRuleException ex = new BusinessRuleException("Advance cannot exceed total price");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Referer", "http://localhost:8080/orders/new");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = exceptionHandler.handleBusinessRule(ex, request, redirectAttributes);

        assertEquals("redirect:http://localhost:8080/orders/new", view);
        assertEquals("Advance cannot exceed total price", redirectAttributes.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void testHandleBusinessRuleWithoutReferer() {
        BusinessRuleException ex = new BusinessRuleException("Order must have at least one garment");
        MockHttpServletRequest request = new MockHttpServletRequest();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = exceptionHandler.handleBusinessRule(ex, request, redirectAttributes);

        assertEquals("redirect:/", view);
        assertEquals("Order must have at least one garment", redirectAttributes.getFlashAttributes().get("errorMessage"));
    }
}
