package com.geetanjali.backend.controller;

import com.geetanjali.backend.dto.draft.OrderDraft;
import com.geetanjali.backend.dto.draft.OrderDraftItem;
import com.geetanjali.backend.dto.form.AdvanceForm;
import com.geetanjali.backend.dto.form.CustomerStepForm;
import com.geetanjali.backend.dto.form.ItemForm;
import com.geetanjali.backend.dto.response.MeasurementPrefill;
import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.entity.GarmentField;
import com.geetanjali.backend.entity.OrderItem;
import com.geetanjali.backend.entity.Payment;
import com.geetanjali.backend.enums.PaymentMode;
import com.geetanjali.backend.exception.ResourceNotFoundException;
import com.geetanjali.backend.security.AppUserDetails;
import com.geetanjali.backend.service.GarmentService;
import com.geetanjali.backend.service.MeasurementService;
import com.geetanjali.backend.service.OrderService;
import com.geetanjali.backend.service.OrderWizardService;
import com.geetanjali.backend.service.PaymentService;
import com.geetanjali.backend.service.storage.PhotoStorageService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final PhotoStorageService photoStorageService;
    private final Clock clock;

    // Constructor injection: Spring provides the service beans and IST Clock automatically.
    public OrderWizardController(OrderWizardService orderWizardService,
                                 GarmentService garmentService,
                                 MeasurementService measurementService,
                                 OrderService orderService,
                                 PaymentService paymentService,
                                 PhotoStorageService photoStorageService,
                                 Clock clock) {
        this.orderWizardService = orderWizardService;
        this.garmentService = garmentService;
        this.measurementService = measurementService;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.photoStorageService = photoStorageService;
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

    // Handles GET /orders/new/summary: Displays summary of all items in the draft cart,
    // plus the advance payment box with Cash/UPI mode and live remaining balance.
    //
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

        if (!model.containsAttribute("advanceForm")) {
            model.addAttribute("advanceForm", new AdvanceForm(BigDecimal.ZERO, PaymentMode.CASH));
        }
        model.addAttribute("draft", draft);

        return "orders/wizard-summary";
    }

    // Handles POST /orders/new/summary: Validates advance deposit and saves the whole order in one @Transactional call.
    // Clears the session draft and redirects to the order success screen.
    //
    // Express comparison: `app.post('/orders/new/summary', async (req, res) => { const order = await orderService.placeOrder(req.session.draft, req.body); req.session.draft = null; res.redirect('/orders/new/success/' + order.id); })`.
    //
    // Inputs:
    //   advanceForm (@Valid @ModelAttribute("advanceForm") AdvanceForm) - submitted advance money and payment mode.
    //   bindingResult (BindingResult) - contains any validation errors.
    //   model (Model) - view model.
    //   redirectAttributes (RedirectAttributes) - flash messages across redirect.
    //   userDetails (@AuthenticationPrincipal AppUserDetails) - logged-in staff member.
    // Output: String - redirect to success screen or re-render summary view on errors.
    //
    // @PostMapping("/summary") = Maps HTTP POST requests for "/orders/new/summary" to this method.
    @PostMapping("/summary")
    public String handleSaveOrder(
            @Valid @ModelAttribute("advanceForm") AdvanceForm advanceForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal AppUserDetails userDetails) {

        OrderDraft draft = orderWizardService.getDraft();

        // Guard: Order must have at least one item
        if (draft.getItemCount() == 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Your cart is empty. Please add at least one garment item.");
            return "redirect:/orders/new/garment";
        }

        // Validate that advance amount does not exceed the total price
        if (advanceForm.getAdvanceAmount() != null && advanceForm.getAdvanceAmount().compareTo(draft.getTotalPrice()) > 0) {
            bindingResult.rejectValue("advanceAmount", "excessAdvance",
                    "Advance amount cannot exceed the order total of ₹" + draft.getTotalPrice());
        }

        // If validation errors exist, re-render summary page
        if (bindingResult.hasErrors()) {
            model.addAttribute("draft", draft);
            return "orders/wizard-summary";
        }

        // Check if we are appending to an existing order
        boolean isAppend = draft.getExistingOrderId() != null;

        // Commit order atomically into PostgreSQL
        CustomerOrder savedOrder = orderService.placeOrder(
                draft,
                advanceForm,
                userDetails.getAppUser()
        );

        // Clear session draft so back button or fresh order starts clean
        orderWizardService.startFresh();

        if (isAppend) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Items added to Order #" + savedOrder.getOrderNo() + " successfully!");
            return "redirect:/orders/" + savedOrder.getId();
        }

        // Always redirect after POST (Post-Redirect-Get pattern)
        redirectAttributes.addFlashAttribute("successMessage",
                "Order #" + savedOrder.getOrderNo() + " booked successfully!");
        return "redirect:/orders/new/success/" + savedOrder.getId();
    }

    // Handles GET /orders/new/success/{orderId}: Displays confirmation screen for a newly placed order.
    // Refreshing this page is safe and cannot double-book or duplicate database rows.
    //
    // Inputs:
    //   orderId (@PathVariable("orderId") Long) - database ID of the placed order.
    //   model (Model) - passes order details and financial totals to Thymeleaf.
    // Output: String - view template path "orders/wizard-done".
    //
    // @GetMapping("/success/{orderId}") = Maps HTTP GET requests for "/orders/new/success/{orderId}".
    @GetMapping("/success/{orderId}")
    public String showSuccessStep(
            @PathVariable("orderId") Long orderId,
            Model model) {

        CustomerOrder order = orderService.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        List<OrderItem> items = orderService.getOrderItems(orderId);
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItem item : items) {
            if (item.getPrice() != null) {
                totalAmount = totalAmount.add(item.getPrice());
            }
        }

        BigDecimal advancePaid = paymentService.getTotalPaid(orderId);
        BigDecimal balanceDue = totalAmount.subtract(advancePaid).max(BigDecimal.ZERO);
        List<Payment> payments = paymentService.findByOrderId(orderId);

        model.addAttribute("order", order);
        model.addAttribute("items", items);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("advancePaid", advancePaid);
        model.addAttribute("balanceDue", balanceDue);
        model.addAttribute("payments", payments);

        return "orders/wizard-done";
    }

    // =========================================================================
    // DRAFT ITEM EDIT & REMOVE ACTIONS
    // =========================================================================

    // Handles GET /orders/new/item/{index}/edit: Re-opens Step 3 for an item in the cart by its 0-based index.
    // Inputs: index (int), model (Model), redirectAttributes (RedirectAttributes).
    // Output: String - view template "orders/wizard-item".
    @GetMapping("/item/{index}/edit")
    public String showEditItem(
            @PathVariable("index") int index,
            Model model,
            RedirectAttributes redirectAttributes) {

        OrderDraft draft = orderWizardService.getDraft();
        if (index < 0 || index >= draft.getItemCount()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid garment item index.");
            return "redirect:/orders/new/summary";
        }

        OrderDraftItem item = draft.getItems().get(index);
        Garment garment = item.getGarment();
        draft.setCurrentGarment(garment);

        List<GarmentField> fields = garmentService.getActiveFields(garment.getId());

        ItemForm form = new ItemForm();
        form.setMeasurements(item.getMeasurementValues());
        form.setNotes(item.getNotes());
        form.setDeliveryDate(item.getDeliveryDate());
        form.setPrice(item.getPrice());
        form.setPhotoUrls(item.getPhotoUrls());
        form.setPhotoPublicIds(item.getPhotoPublicIds());

        LocalDate today = clock.instant().atZone(clock.getZone()).toLocalDate();

        model.addAttribute("itemForm", form);
        model.addAttribute("garment", garment);
        model.addAttribute("garmentFields", fields);
        model.addAttribute("draft", draft);
        model.addAttribute("minDate", today.toString());
        model.addAttribute("isEdit", true);
        model.addAttribute("editIndex", index);

        return "orders/wizard-item";
    }

    // Handles POST /orders/new/item/{index}: Updates an existing item in the draft cart.
    // Inputs: index (int), itemForm (ItemForm), bindingResult (BindingResult), model (Model), redirectAttributes (RedirectAttributes).
    // Output: String - redirect to /orders/new/summary.
    @PostMapping("/item/{index}")
    public String handleUpdateItem(
            @PathVariable("index") int index,
            @Valid @ModelAttribute("itemForm") ItemForm itemForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        OrderDraft draft = orderWizardService.getDraft();
        if (index < 0 || index >= draft.getItemCount()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid garment item index.");
            return "redirect:/orders/new/summary";
        }

        LocalDate today = clock.instant().atZone(clock.getZone()).toLocalDate();
        if (itemForm.getDeliveryDate() != null && itemForm.getDeliveryDate().isBefore(today)) {
            bindingResult.rejectValue("deliveryDate", "pastDate", "Promised delivery date cannot be in the past");
        }

        if (bindingResult.hasErrors()) {
            OrderDraftItem item = draft.getItems().get(index);
            Garment garment = item.getGarment();
            List<GarmentField> fields = garmentService.getActiveFields(garment.getId());
            model.addAttribute("garment", garment);
            model.addAttribute("garmentFields", fields);
            model.addAttribute("draft", draft);
            model.addAttribute("minDate", today.toString());
            model.addAttribute("isEdit", true);
            model.addAttribute("editIndex", index);
            return "orders/wizard-item";
        }

        orderWizardService.updateItem(index, itemForm);
        redirectAttributes.addFlashAttribute("successMessage", "Updated garment item in order draft.");
        return "redirect:/orders/new/summary";
    }

    // Handles POST /orders/new/item/{index}/remove: Deletes an item and cleans up its photo files from disk.
    // Inputs: index (int), redirectAttributes (RedirectAttributes).
    // Output: String - redirect to /orders/new/summary.
    @PostMapping("/item/{index}/remove")
    public String handleRemoveItem(
            @PathVariable("index") int index,
            RedirectAttributes redirectAttributes) {

        orderWizardService.removeItemAndPhotos(index, photoStorageService);
        redirectAttributes.addFlashAttribute("successMessage", "Removed garment item from order draft.");
        return "redirect:/orders/new/summary";
    }
}
