package com.geetanjali.backend.controller;

import com.geetanjali.backend.dto.draft.OrderDraft;
import com.geetanjali.backend.dto.draft.OrderDraftItem;
import com.geetanjali.backend.dto.form.CustomerStepForm;
import com.geetanjali.backend.dto.form.ItemForm;
import com.geetanjali.backend.dto.response.MeasurementPrefill;
import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.entity.GarmentField;
import com.geetanjali.backend.service.GarmentService;
import com.geetanjali.backend.service.MeasurementService;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    private final MeasurementService measurementService;
    private final Clock clock;

    // Constructor injection: Spring provides the service beans and IST Clock automatically.
    // Inputs:
    //   orderWizardService (OrderWizardService)
    //   garmentService (GarmentService)
    //   measurementService (MeasurementService)
    //   clock (Clock) - always Asia/Kolkata timezone
    public OrderWizardController(OrderWizardService orderWizardService,
                                 GarmentService garmentService,
                                 MeasurementService measurementService,
                                 Clock clock) {
        this.orderWizardService = orderWizardService;
        this.garmentService = garmentService;
        this.measurementService = measurementService;
        this.clock = clock;
    }

    // =========================================================================
    // STEP 1: CUSTOMER IDENTIFICATION
    // =========================================================================

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
    // @PostMapping("/customer") = Maps HTTP POST requests for "/orders/new/customer" to this method.
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

    // =========================================================================
    // STEP 2: GARMENT SELECTION
    // =========================================================================

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
    // Selects the garment, creates an active draft item, and advances to Step 3 (measurements).
    //
    // Express comparison: `app.post('/orders/new/garment', (req, res) => { draft.selectGarment(id); res.redirect('/orders/new/item'); })`.
    //
    // Inputs:
    //   garmentId (@RequestParam("garmentId") Long) - chosen garment primary key ID.
    //   redirectAttributes (RedirectAttributes) - flash messages.
    // Output: String - redirect to /orders/new/item.
    //
    // @PostMapping("/garment") = Maps HTTP POST requests for "/orders/new/garment" to this method.
    @PostMapping("/garment")
    public String handleGarmentStep(
            @RequestParam("garmentId") Long garmentId,
            RedirectAttributes redirectAttributes) {

        orderWizardService.selectGarment(garmentId);

        // Advance to Step 3: item details and measurements
        return "redirect:/orders/new/item";
    }

    // =========================================================================
    // STEP 3: ITEM DETAILS & MEASUREMENTS
    // =========================================================================

    // Handles GET /orders/new/item: Displays Wizard Step 3: Item details and measurements form.
    // 1. Checks that a customer and garment are currently selected in the session draft.
    // 2. Loads active fields for the garment from GarmentService.
    // 3. For repeat customers, queries MeasurementService for the newest snapshot to prefill numbers.
    // 4. Sets a default delivery date (+7 days from today in IST) and price.
    //
    // Inputs: model (Model), redirectAttributes (RedirectAttributes).
    // Output: String - view name "orders/wizard-item".
    //
    // @GetMapping("/item") = Maps HTTP GET requests for "/orders/new/item" to this method.
    @GetMapping("/item")
    public String showItemStep(Model model, RedirectAttributes redirectAttributes) {
        OrderDraft draft = orderWizardService.getDraft();

        // Guard 1: Verify customer exists in draft
        if (!draft.hasCustomer()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please identify the customer first.");
            return "redirect:/orders/new";
        }

        // Guard 2: Verify garment was selected in Step 2
        Garment garment = draft.getCurrentGarment();
        if (garment == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a garment category first.");
            return "redirect:/orders/new/garment";
        }

        // Fetch measurement field templates for this garment
        List<GarmentField> fields = garmentService.getActiveFields(garment.getId());

        // Prepare ItemForm backing object
        ItemForm itemForm = new ItemForm();

        // Today always comes from the injected Clock (Asia/Kolkata)
        LocalDate today = clock.instant().atZone(clock.getZone()).toLocalDate();
        itemForm.setDeliveryDate(today.plusDays(7)); // Default 1 week turnaround
        itemForm.setPrice(BigDecimal.ZERO);

        // Check for prefill measurements from newest snapshot
        MeasurementPrefill prefillInfo = null;
        if (draft.getCustomerId() != null) {
            Optional<MeasurementPrefill> prefillOpt = measurementService.getPrefill(draft.getCustomerId(), garment.getId());
            if (prefillOpt.isPresent()) {
                prefillInfo = prefillOpt.get();
                // Prefill existing measurement numbers
                itemForm.setMeasurements(prefillInfo.values());
            }
        }

        model.addAttribute("itemForm", itemForm);
        model.addAttribute("garment", garment);
        model.addAttribute("garmentFields", fields);
        model.addAttribute("draft", draft);
        model.addAttribute("prefillInfo", prefillInfo);
        model.addAttribute("minDate", today.toString());

        return "orders/wizard-item";
    }

    // Handles POST /orders/new/item: Processes and validates measurements, styling notes, delivery date, and price.
    // On validation errors: redisplays Step 3 with inline error messages.
    // On success: copies values into an OrderDraftItem and adds it to the session cart.
    // Depending on the submit button clicked:
    // - action="more" -> redirects to /orders/new/garment to add another garment item.
    // - action="finish" -> redirects to /orders/new/summary to review the cart.
    //
    // Inputs:
    //   itemForm (@Valid @ModelAttribute("itemForm") ItemForm) - submitted form data.
    //   bindingResult (BindingResult) - contains any validation constraint violations.
    //   model (Model) - view model.
    //   redirectAttributes (RedirectAttributes) - flash messages across redirect.
    // Output: String - view template path or redirect string.
    //
    // @PostMapping("/item") = Maps HTTP POST requests for "/orders/new/item" to this method.
    @PostMapping("/item")
    public String handleItemStep(
            @Valid @ModelAttribute("itemForm") ItemForm itemForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        OrderDraft draft = orderWizardService.getDraft();
        Garment garment = draft.getCurrentGarment();

        // Server-side validation: delivery date cannot be in the past (using injected Clock)
        LocalDate today = clock.instant().atZone(clock.getZone()).toLocalDate();
        if (itemForm.getDeliveryDate() != null && itemForm.getDeliveryDate().isBefore(today)) {
            bindingResult.rejectValue("deliveryDate", "pastDate", "Promised delivery date cannot be in the past");
        }

        // If validation errors exist, re-populate context and redisplay form
        if (bindingResult.hasErrors()) {
            if (garment != null) {
                List<GarmentField> fields = garmentService.getActiveFields(garment.getId());
                model.addAttribute("garment", garment);
                model.addAttribute("garmentFields", fields);
            }
            model.addAttribute("draft", draft);
            model.addAttribute("minDate", today.toString());
            return "orders/wizard-item";
        }

        // Save item into session draft
        OrderDraftItem savedItem = orderWizardService.saveItem(itemForm);

        // Branch based on action button clicked
        if ("more".equalsIgnoreCase(itemForm.getAction())) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Added " + savedItem.getGarment().getName() + " to order! Choose your next garment category.");
            return "redirect:/orders/new/garment";
        } else {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Added " + savedItem.getGarment().getName() + " to order draft!");
            return "redirect:/orders/new/summary";
        }
    }

    // =========================================================================
    // STEP 4: ORDER DRAFT REVIEW & SUMMARY
    // =========================================================================

    // Handles GET /orders/new/summary: Displays summary of all items in the draft cart.
    // Inputs: model (Model), redirectAttributes (RedirectAttributes).
    // Output: String - view name "orders/wizard-summary".
    //
    // @GetMapping("/summary") = Maps HTTP GET requests for "/orders/new/summary" to this method.
    @GetMapping("/summary")
    public String showSummaryStep(Model model, RedirectAttributes redirectAttributes) {
        OrderDraft draft = orderWizardService.getDraft();

        if (!draft.hasCustomer()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select customer details first.");
            return "redirect:/orders/new";
        }

        if (draft.getItemCount() == 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Your cart is empty. Please add at least one garment item.");
            return "redirect:/orders/new/garment";
        }

        model.addAttribute("draft", draft);

        return "orders/wizard-summary";
    }
}
