package com.geetanjali.backend.controller;

import com.geetanjali.backend.dto.form.ItemForm;
import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.entity.GarmentField;
import com.geetanjali.backend.entity.OrderItem;
import com.geetanjali.backend.service.GarmentService;
import com.geetanjali.backend.service.OrderService;
import com.geetanjali.backend.service.OrderWizardService;
import jakarta.validation.Valid;
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
    private final Clock clock;

    // Constructor injection: Spring automatically injects dependencies.
    // Inputs: orderService (OrderService), orderWizardService (OrderWizardService), garmentService (GarmentService), clock (Clock).
    public OrderController(OrderService orderService,
                           OrderWizardService orderWizardService,
                           GarmentService garmentService,
                           Clock clock) {
        this.orderService = orderService;
        this.orderWizardService = orderWizardService;
        this.garmentService = garmentService;
        this.clock = clock;
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

        return "orders/detail";
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
