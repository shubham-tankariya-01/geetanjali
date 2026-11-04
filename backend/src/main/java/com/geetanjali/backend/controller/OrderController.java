package com.geetanjali.backend.controller;

import com.geetanjali.backend.dto.calendar.DayCapacityDto;
import com.geetanjali.backend.dto.form.ItemForm;
import com.geetanjali.backend.dto.form.OverrideForm;
import com.geetanjali.backend.dto.form.PaymentForm;
import com.geetanjali.backend.entity.AppUser;
import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.entity.GarmentField;
import com.geetanjali.backend.entity.OrderItem;
import com.geetanjali.backend.exception.BalancePendingException;
import com.geetanjali.backend.exception.BusinessRuleException;
import com.geetanjali.backend.security.AppUserDetails;
import com.geetanjali.backend.service.CalendarService;
import com.geetanjali.backend.service.DeliveryService;
import com.geetanjali.backend.service.GarmentService;
import com.geetanjali.backend.service.OrderService;
import com.geetanjali.backend.service.OrderWizardService;
import com.geetanjali.backend.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

// OrderController: Web MVC controller handling HTTP requests for viewing, editing, and appending items to customer orders.
// In Express, this corresponds to routes in `routes/orders.js`.
//
// CONTROLLER THINNESS RULE:
// Controllers only read the request, validate/extract parameters, invoke ONE service method, and select the view.
// They never contain business calculations or query database repositories directly.
//
// @Controller = Tells Spring MVC that this class handles web requests and returns Thymeleaf template view names.
@Controller
// @RequestMapping = Sets the base URL path for this controller to "/orders".
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderWizardService orderWizardService;
    private final GarmentService garmentService;
    private final PaymentService paymentService;
    private final DeliveryService deliveryService;
    private final Clock clock;
    private final CalendarService calendarService;

    // Full constructor injection with CalendarService
    @Autowired
    public OrderController(OrderService orderService,
                           OrderWizardService orderWizardService,
                           GarmentService garmentService,
                           PaymentService paymentService,
                           DeliveryService deliveryService,
                           Clock clock,
                           CalendarService calendarService) {
        this.orderService = orderService;
        this.orderWizardService = orderWizardService;
        this.garmentService = garmentService;
        this.paymentService = paymentService;
        this.deliveryService = deliveryService;
        this.clock = clock;
        this.calendarService = calendarService;
    }

    // Convenience constructor for tests that inject 6 arguments.
    public OrderController(OrderService orderService,
                           OrderWizardService orderWizardService,
                           GarmentService garmentService,
                           PaymentService paymentService,
                           DeliveryService deliveryService,
                           Clock clock) {
        this(orderService, orderWizardService, garmentService, paymentService, deliveryService, clock, null);
    }

    // Convenience constructor for tests that do not inject DeliveryService.
    public OrderController(OrderService orderService,
                           OrderWizardService orderWizardService,
                           GarmentService garmentService,
                           PaymentService paymentService,
                           Clock clock) {
        this(orderService, orderWizardService, garmentService, paymentService, null, clock, null);
    }

    // Convenience constructor for tests that do not test payment or delivery endpoints.
    public OrderController(OrderService orderService,
                           OrderWizardService orderWizardService,
                           GarmentService garmentService,
                           Clock clock) {
        this(orderService, orderWizardService, garmentService, null, null, clock, null);
    }

    // Handles GET /orders: Displays the customer orders overview list in the All Orders section.
    // Fetches active orders sorted ascending by earliest delivery date, excluding orders where both payment and delivery are completed.
    // Inputs: model (Model).
    // Output: String - Thymeleaf template path "orders/index".
    // @GetMapping = Maps HTTP GET requests for "/orders" to this method.
    @GetMapping
    public String listOrders(Model model) {
        List<CustomerOrder> orders = orderService.getActiveOrdersSortedByDeliveryDate();
        model.addAttribute("orders", orders);
        return "orders/index";
    }

    // Handles GET /orders/{id}: Displays the complete order details page.
    // Shows the customer info header, financial summary, and item cards with photos and measurements.
    // If the order does not exist, orderService throws ResourceNotFoundException which GlobalExceptionHandler maps to HTTP 404.
    //
    // Express comparison: `app.get('/orders/:id', async (req, res) => { const order = await orderService.get(req.params.id); res.render('orders/detail', { order }); });`
    //
    // Inputs:
    //   id (@PathVariable("id") Long) - primary key ID of the CustomerOrder.
    //   model (Model) - passes view attributes to the Thymeleaf template.
    // Output: String - Thymeleaf template path "orders/detail".
    //
    // @GetMapping("/{id}") = Maps HTTP GET requests for "/orders/{id}" to this method.
    @GetMapping("/{id}")
    public String showOrderDetail(@PathVariable("id") Long id, Model model) {
        // Fetch order through service layer. Throws ResourceNotFoundException (404) if missing.
        CustomerOrder order = orderService.getOrderOrThrow(id);

        // Pass entity directly to template (open-in-view allows on-demand lazy loading of items, photos, and measurements)
        model.addAttribute("order", order);

        // Pass today's date from the injected Clock (Asia/Kolkata)
        model.addAttribute("today", clock.instant().atZone(clock.getZone()).toLocalDate());

        // Provide empty OverrideForm DTO for override modal form binding if not already set
        if (!model.containsAttribute("overrideForm")) {
            model.addAttribute("overrideForm", new OverrideForm());
        }

        return "orders/detail";
    }

    // Handles POST /orders/{id}/payments: Records a new payment towards an order.
    // Enforces: amount > 0, amount <= current balance, date not in future.
    // Inputs:
    //   id (@PathVariable("id") Long) - primary key ID of the CustomerOrder.
    //   form (@Valid @ModelAttribute("paymentForm") PaymentForm) - form DTO containing payment details.
    //   bindingResult (BindingResult) - validation errors holder.
    //   redirectAttributes (RedirectAttributes) - flash messages.
    // Output: String - redirect to safe return URL.
    //
    // @PostMapping("/{id}/payments") = Maps HTTP POST requests for adding payments.
    @PostMapping("/{id}/payments")
    public String addPayment(@PathVariable("id") Long id,
                             @jakarta.validation.Valid @ModelAttribute("paymentForm") com.geetanjali.backend.dto.form.PaymentForm form,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        String targetUrl = resolveSafeReturnUrl(form != null ? form.getReturnTo() : null, id);

        // Server-side form validation error
        if (bindingResult.hasErrors()) {
            String firstError = bindingResult.getAllErrors().getFirst().getDefaultMessage();
            redirectAttributes.addFlashAttribute("errorMessage", firstError);
            return "redirect:" + targetUrl;
        }

        try {
            paymentService.addPayment(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Payment recorded successfully.");
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:" + targetUrl;
    }

    // Resolves and sanitizes the return URL to prevent open-redirect vulnerabilities.
    // Accepts returnTo ONLY if it starts with a single '/' and never '//' or 'http'.
    // Defaults to "/orders/" + orderId.
    private String resolveSafeReturnUrl(String returnTo, Long orderId) {
        if (returnTo != null && returnTo.startsWith("/") && !returnTo.startsWith("//") && !returnTo.toLowerCase().contains("http")) {
            return returnTo;
        }
        return "/orders/" + orderId;
    }

    // Handles POST /orders/{id}/items/{itemId}/done: Marks a garment item as stitched (DONE).
    // Precondition: Only allowed when item status is PENDING.
    // Authorized for both TAILOR and OWNER roles.
    // Inputs:
    //   id (@PathVariable("id") Long) - Order ID.
    //   itemId (@PathVariable("itemId") Long) - Order item ID.
    //   redirectAttributes (RedirectAttributes) - Passes flash messages across redirect.
    // Output: String - Redirect to "/orders/" + id.
    //
    // Maps incoming HTTP POST requests for marking an item done to this handler method.
    @PostMapping("/{id}/items/{itemId}/done")
    public String markItemDone(@PathVariable("id") Long id,
                               @PathVariable("itemId") Long itemId,
                               RedirectAttributes redirectAttributes) {
        try {
            orderService.markDone(id, itemId);
            redirectAttributes.addFlashAttribute("successMessage", "Item marked as completed (stitching done).");
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    // Handles POST /orders/{id}/items/{itemId}/deliver: Marks a garment item as delivered to the customer.
    // Preconditions: Item is in PENDING or DONE; order balance must be zero.
    // If order has an unpaid balance, deliveryService throws BalancePendingException,
    // which triggers the red balance warning panel.
    // Inputs:
    //   id (@PathVariable("id") Long) - Order ID.
    //   itemId (@PathVariable("itemId") Long) - Garment item ID.
    //   redirectAttributes (RedirectAttributes) - Passes flash attributes across redirect.
    // Output: String - Redirect to "/orders/" + id.
    //
    // Maps incoming HTTP POST requests for delivering a garment to this handler method.
    @PostMapping("/{id}/items/{itemId}/deliver")
    public String deliverItem(@PathVariable("id") Long id,
                             @PathVariable("itemId") Long itemId,
                             RedirectAttributes redirectAttributes) {
        try {
            deliveryService.deliverItem(id, itemId);
            redirectAttributes.addFlashAttribute("successMessage", "Item delivered successfully.");
        } catch (BalancePendingException ex) {
            // Flash blocked item ID and exact pending balance to display the warning panel on redirect
            redirectAttributes.addFlashAttribute("blockedItemId", itemId);
            redirectAttributes.addFlashAttribute("blockedBalance", ex.getBalance());
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    // Handles POST /orders/{id}/items/{itemId}/override: Allows shop OWNER to authorize delivery despite unpaid balance.
    // Preconditions: Authenticated user must have OWNER role; reason must not be blank (max 500 chars).
    // Inputs:
    //   id (@PathVariable("id") Long) - Order ID.
    //   itemId (@PathVariable("itemId") Long) - Garment item ID.
    //   form (@Valid @ModelAttribute("overrideForm") OverrideForm) - Form DTO with justification reason.
    //   bindingResult (BindingResult) - Holds validation errors.
    //   userDetails (@AuthenticationPrincipal AppUserDetails) - Current logged-in staff member.
    //   redirectAttributes (RedirectAttributes) - Passes flash messages.
    // Output: String - Redirect to "/orders/" + id.
    //
    // Maps incoming HTTP POST requests for owner delivery overrides to this handler method.
    @PostMapping("/{id}/items/{itemId}/override")
    public String deliverWithOverride(@PathVariable("id") Long id,
                                      @PathVariable("itemId") Long itemId,
                                      @Valid @ModelAttribute("overrideForm") OverrideForm form,
                                      BindingResult bindingResult,
                                      @AuthenticationPrincipal AppUserDetails userDetails,
                                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            redirectAttributes.addFlashAttribute("blockedItemId", itemId);
            return "redirect:/orders/" + id;
        }

        try {
            AppUser currentUser = (userDetails != null) ? userDetails.getUser() : null;
            deliveryService.deliverWithOverride(id, itemId, form.getReason(), currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Item delivered with owner override.");
        } catch (AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only shop OWNER can authorize a delivery override.");
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    // Handles GET /orders/{orderId}/add-items: Starts the order wizard to append new items to an existing order.
    // Pre-populates the customer into the session draft, sets the existingOrderId, and skips Step 1 by redirecting directly to Step 2.
    //
    // Inputs:
    //   orderId (@PathVariable("orderId") Long) - ID of the order to add items to.
    //   redirectAttributes (RedirectAttributes) - flash messages.
    // Output: String - redirect to "/orders/new/garment".
    //
    // @GetMapping("/{orderId}/add-items") = Maps HTTP GET requests for appending items to an existing order.
    @GetMapping("/{orderId}/add-items")
    public String startAddItemsToOrder(
            @PathVariable("orderId") Long orderId,
            RedirectAttributes redirectAttributes) {

        CustomerOrder order = orderService.getOrderOrThrow(orderId);
        orderWizardService.startAppendToOrder(order);

        redirectAttributes.addFlashAttribute("successMessage",
                "Adding items to Order #" + order.getOrderNo() + " for " + order.getCustomer().getName() + ".");

        return "redirect:/orders/new/garment";
    }

    // Handles GET /orders/{orderId}/items/{itemId}/edit: Displays the item edit form pre-filled with saved numbers, notes, price, and photos.
    //
    // Inputs:
    //   orderId (@PathVariable("orderId") Long) - ID of the parent order.
    //   itemId (@PathVariable("itemId") Long) - ID of the order item to edit.
    //   model (Model) - view model.
    // Output: String - Thymeleaf template path "orders/item-edit".
    //
    // @GetMapping("/{orderId}/items/{itemId}/edit") = Maps HTTP GET requests for editing an item.
    @GetMapping("/{orderId}/items/{itemId}/edit")
    public String showEditOrderItem(
            @PathVariable("orderId") Long orderId,
            @PathVariable("itemId") Long itemId,
            Model model) {

        CustomerOrder order = orderService.getOrderOrThrow(orderId);
        OrderItem item = orderService.getOrderItemOrThrow(orderId, itemId);

        ItemForm form = orderService.prepareItemEditForm(item);
        List<GarmentField> fields = garmentService.getActiveFields(item.getGarment().getId());

        // "Today" always comes from the injected Clock (Asia/Kolkata)
        LocalDate today = clock.instant().atZone(clock.getZone()).toLocalDate();

        model.addAttribute("order", order);
        model.addAttribute("item", item);
        model.addAttribute("itemForm", form);
        model.addAttribute("garment", item.getGarment());
        model.addAttribute("garmentFields", fields);
        model.addAttribute("minDate", today.toString());
        model.addAttribute("formAction", "/orders/" + orderId + "/items/" + itemId);
        model.addAttribute("cancelUrl", "/orders/" + orderId);
        model.addAttribute("isEdit", true);

        return "orders/item-edit";
    }

    // Handles POST /orders/{orderId}/items/{itemId}: Saves edits to an existing order item.
    // If measurements changed, creates a fresh measurement snapshot while keeping the old snapshot intact.
    //
    // Inputs:
    //   orderId (@PathVariable("orderId") Long) - ID of parent order.
    //   itemId (@PathVariable("itemId") Long) - ID of order item being updated.
    //   form (@Valid @ModelAttribute("itemForm") ItemForm) - submitted form data.
    //   bindingResult (BindingResult) - validation results.
    //   model (Model) - view model in case of error redisplay.
    //   redirectAttributes (RedirectAttributes) - flash messages across redirect.
    // Output: String - redirect to order detail or redisplay edit view on error.
    //
    // @PostMapping("/{orderId}/items/{itemId}") = Maps HTTP POST requests for item update.
    @PostMapping("/{orderId}/items/{itemId}")
    public String handleUpdateOrderItem(
            @PathVariable("orderId") Long orderId,
            @PathVariable("itemId") Long itemId,
            @Valid @ModelAttribute("itemForm") ItemForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Validate delivery date is not in past using injected Clock
        LocalDate today = clock.instant().atZone(clock.getZone()).toLocalDate();
        if (form.getDeliveryDate() != null && form.getDeliveryDate().isBefore(today)) {
            bindingResult.rejectValue("deliveryDate", "pastDate", "Promised delivery date cannot be in the past");
        } else if (form.getDeliveryDate() != null && calendarService != null) {
            DayCapacityDto capacity = calendarService.getDayCapacity(form.getDeliveryDate());
            if (capacity.maxReached()) {
                bindingResult.rejectValue("deliveryDate", "capacityExceeded",
                        "Maximum daily capacity reached (10 orders already scheduled for this date). Please choose another date.");
            }
        }

        if (bindingResult.hasErrors()) {
            CustomerOrder order = orderService.getOrderOrThrow(orderId);
            OrderItem item = orderService.getOrderItemOrThrow(orderId, itemId);
            List<GarmentField> fields = garmentService.getActiveFields(item.getGarment().getId());

            model.addAttribute("order", order);
            model.addAttribute("item", item);
            model.addAttribute("garment", item.getGarment());
            model.addAttribute("garmentFields", fields);
            model.addAttribute("minDate", today.toString());
            model.addAttribute("formAction", "/orders/" + orderId + "/items/" + itemId);
            model.addAttribute("cancelUrl", "/orders/" + orderId);
            model.addAttribute("isEdit", true);

            return "orders/item-edit";
        }

        orderService.updateOrderItem(orderId, itemId, form);

        redirectAttributes.addFlashAttribute("successMessage", "Item updated successfully!");
        return "redirect:/orders/" + orderId;
    }
}
