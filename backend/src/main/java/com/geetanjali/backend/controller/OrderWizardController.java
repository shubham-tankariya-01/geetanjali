package com.geetanjali.backend.controller;

import com.geetanjali.backend.dto.draft.OrderDraft;
import com.geetanjali.backend.dto.form.CustomerStepForm;
import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.service.GarmentService;
import com.geetanjali.backend.service.OrderWizardService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

// OrderWizardController: Manages HTTP web navigation and state transitions for the multi-step order wizard.
// In Express, this corresponds to `router.get('/orders/new', ...)` and `router.post('/orders/new/customer', ...)`.
// Kept strictly thin: validates input, calls ONE service method, and returns a view name or redirect URL.
//
// @Controller = Tells Spring MVC to manage this class as a web controller returning Thymeleaf view templates.
@Controller
// @RequestMapping = Sets the base URL path for the wizard endpoints ("/orders/new").
@RequestMapping("/orders/new")
public class OrderWizardController {

    private final OrderWizardService orderWizardService;
    private final GarmentService garmentService;

    // Constructor injection: Spring provides the service beans automatically.
    // Inputs: orderWizardService (OrderWizardService), garmentService (GarmentService).
    public OrderWizardController(OrderWizardService orderWizardService, GarmentService garmentService) {
        this.orderWizardService = orderWizardService;
        this.garmentService = garmentService;
    }

    // Handles GET /orders/new: Entry point for the order wizard.
    // Resets the session draft to a clean slate (or pre-populates it for a known customer if ?customerId=42 is provided),
    // and displays Wizard Step 1: Customer Identification.
    //
    // Express comparison: `app.get('/orders/new', (req, res) => { req.session.draft = {}; res.render('wizard-customer'); })`.
    //
    // Inputs:
    //   customerId (@RequestParam(value="customerId", required=false) Long) - optional customer ID from customer detail page.
    //   model (Model) - passes form backing object to Thymeleaf.
    // Output: String - Thymeleaf view name "orders/wizard-customer".
    //
    // @GetMapping = Maps HTTP GET requests for "/orders/new" to this method.
    @GetMapping
    public String showCustomerStep(
            @RequestParam(value = "customerId", required = false) Long customerId,
            Model model) {

        if (customerId != null) {
            // Start wizard pre-populated for known customer
            orderWizardService.startForCustomer(customerId);
        } else {
            // Start completely fresh
            orderWizardService.startFresh();
        }

        // Initialize form with customer details from draft
        OrderDraft draft = orderWizardService.getDraft();
        CustomerStepForm form = new CustomerStepForm(
                draft.getCustomerId(),
                draft.getCustomerMobile(),
                draft.getCustomerName(),
                draft.getExistingOrderId()
        );

        model.addAttribute("customerStepForm", form);

        return "orders/wizard-customer";
    }

    // Handles POST /orders/new/customer: Processes and validates customer identification in Step 1.
    // Re-renders Step 1 with field error banners if validation fails.
    // Otherwise, stores customer info in the session draft and redirects to Step 2 (garment selection).
    //
    // Express comparison: `app.post('/orders/new/customer', (req, res) => { ...; res.redirect('/orders/new/garment'); })`.
    //
    // Inputs:
    //   form (@Valid @ModelAttribute("customerStepForm") CustomerStepForm) - submitted customer form.
    //   bindingResult (BindingResult) - contains any validation constraint violations.
    //   model (Model) - view model.
    //   redirectAttributes (RedirectAttributes) - flash messages across redirect.
    // Output: String - view template path or redirect string.
    //
    // @PostMapping = Maps HTTP POST requests for "/orders/new/customer" to this method.
    @PostMapping("/customer")
    public String handleCustomerStep(
            @Valid @ModelAttribute("customerStepForm") CustomerStepForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "orders/wizard-customer";
        }

        // Save customer identification into session draft
        orderWizardService.setCustomer(form);

        // Always redirect after POST to avoid duplicate form submission on page refresh
        return "redirect:/orders/new/garment";
    }

    // Handles GET /orders/new/garment: Displays Wizard Step 2: Garment category selection.
    // Guards against skipping Step 1: if no customer is set in the draft, redirects back to /orders/new.
    //
    // Express comparison: `app.get('/orders/new/garment', (req, res) => res.render('wizard-garment', { garments, draft }))`.
    //
    // Inputs: model (Model), redirectAttributes (RedirectAttributes).
    // Output: String - view name "orders/wizard-garment" or redirect back to step 1.
    //
    // @GetMapping("/garment") = Maps HTTP GET requests for "/orders/new/garment" to this method.
    @GetMapping("/garment")
    public String showGarmentStep(Model model, RedirectAttributes redirectAttributes) {
        OrderDraft draft = orderWizardService.getDraft();

        // Guard: ensure customer was selected in Step 1
        if (!draft.hasCustomer()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please identify the customer first before choosing a garment.");
            return "redirect:/orders/new";
        }

        // Fetch active garments in display order (Shirt, Pant, Kurta, Blouse)
        List<Garment> garments = garmentService.listActiveGarments();

        model.addAttribute("garments", garments);
        model.addAttribute("draft", draft);

        return "orders/wizard-garment";
    }

    // Handles POST /orders/new/garment: Stores selected garment in session draft.
    // Selects the garment, creates an active draft item, and prepares the draft for Step 3.
    //
    // Express comparison: `app.post('/orders/new/garment', (req, res) => { draft.selectGarment(id); res.redirect(...); })`.
    //
    // Inputs:
    //   garmentId (@RequestParam("garmentId") Long) - chosen garment primary key ID.
    //   redirectAttributes (RedirectAttributes) - flash messages.
    // Output: String - redirect string.
    //
    // @PostMapping("/garment") = Maps HTTP POST requests for "/orders/new/garment" to this method.
    @PostMapping("/garment")
    public String handleGarmentStep(
            @RequestParam("garmentId") Long garmentId,
            RedirectAttributes redirectAttributes) {

        Garment garment = orderWizardService.selectGarment(garmentId);

        redirectAttributes.addFlashAttribute("successMessage",
                "Selected " + garment.getName() + " for " + orderWizardService.getDraft().getCustomerName() + "! Ready for measurement entry in Step 3.");

        // For Step 4, redirects back to garment screen with confirmation (Step 5 will advance to /orders/new/item)
        return "redirect:/orders/new/garment";
    }
}
