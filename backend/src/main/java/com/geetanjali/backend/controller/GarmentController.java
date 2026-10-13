package com.geetanjali.backend.controller;

import com.geetanjali.backend.dto.form.FieldForm;
import com.geetanjali.backend.dto.form.GarmentForm;
import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.entity.GarmentField;
import com.geetanjali.backend.exception.BusinessRuleException;
import com.geetanjali.backend.service.GarmentService;
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

import java.util.List;

// GarmentController: Web MVC controller handling garment category and template configuration.
// Protected by Spring Security URL rule: restricted to users with role OWNER.
// In Express, this corresponds to `settingsRouter.use(requireOwner)` managing `/settings/garments`.
//
// Layering rule: Keeps controllers thin: parses HTTP request, validates, calls ONE service method, and returns a view name or redirect.
// Never directly injects or queries repositories.
//
// @Controller = Tells Spring MVC that this class handles HTTP requests and renders HTML views.
@Controller
// @RequestMapping("/settings/garments") = Base URL prefix for all garment and template settings endpoints.
@RequestMapping("/settings/garments")
public class GarmentController {

    private final GarmentService garmentService;

    // Constructor injection: Spring supplies the GarmentService bean.
    // Input: garmentService (GarmentService).
    public GarmentController(GarmentService garmentService) {
        this.garmentService = garmentService;
    }

    // Handles GET /settings/garments: Displays the list of active garment categories and the add category form.
    // Inputs: model (Model) - transfers categories and form backing object to the Thymeleaf template.
    // Output: String - Thymeleaf template path "settings/garments".
    // @GetMapping = Maps HTTP GET requests for "/settings/garments".
    @GetMapping
    public String showGarments(Model model) {
        List<Garment> garments = garmentService.listActiveGarments();
        model.addAttribute("garments", garments);

        if (!model.containsAttribute("garmentForm")) {
            model.addAttribute("garmentForm", new GarmentForm());
        }

        return "settings/garments";
    }

    // Handles POST /settings/garments: Creates a new garment category.
    // Rejects duplicate names and appends the new category to the end of the sort order.
    // Inputs: form (@Valid GarmentForm), bindingResult (BindingResult), model (Model), redirectAttributes (RedirectAttributes).
    // Output: String - Redirect to "/settings/garments" on success, or re-renders "settings/garments" on validation error.
    // @PostMapping = Maps HTTP POST requests for "/settings/garments".
    @PostMapping
    public String addGarment(@Valid @ModelAttribute("garmentForm") GarmentForm form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("garments", garmentService.listActiveGarments());
            return "settings/garments";
        }

        try {
            Garment garment = garmentService.addGarment(form);
            redirectAttributes.addFlashAttribute("successMessage", "Garment category '" + garment.getName() + "' added successfully.");
            return "redirect:/settings/garments";
        } catch (BusinessRuleException ex) {
            bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            model.addAttribute("garments", garmentService.listActiveGarments());
            return "settings/garments";
        }
    }

    // Handles POST /settings/garments/{id}/remove: Soft-deletes a category by setting active = false.
    // Preserves database integrity so past customer orders and measurements still retain valid foreign keys.
    // Inputs: id (@PathVariable Long), redirectAttributes (RedirectAttributes).
    // Output: String - Redirect to "/settings/garments".
    // @PostMapping("/{id}/remove") = Maps HTTP POST requests for "/settings/garments/{id}/remove".
    @PostMapping("/{id}/remove")
    public String removeGarment(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        garmentService.removeGarment(id);
        redirectAttributes.addFlashAttribute("successMessage", "Garment category removed successfully.");
        return "redirect:/settings/garments";
    }

    // Handles GET /settings/garments/{id}: Displays the measurement template fields for a category.
    // Inputs: id (@PathVariable Long), model (Model).
    // Output: String - Thymeleaf template path "settings/garment-template".
    // @GetMapping("/{id}") = Maps HTTP GET requests for "/settings/garments/{id}".
    @GetMapping("/{id}")
    public String showGarmentTemplate(@PathVariable("id") Long id, Model model) {
        Garment garment = garmentService.getGarmentById(id);
        List<GarmentField> fields = garmentService.getActiveFields(id);

        model.addAttribute("garment", garment);
        model.addAttribute("fields", fields);

        if (!model.containsAttribute("fieldForm")) {
            model.addAttribute("fieldForm", new FieldForm("", "inch"));
        }

        return "settings/garment-template";
    }

    // Handles POST /settings/garments/{id}/fields: Adds a new measurement field to the category template.
    // Places the new field at the end of the template.
    // Inputs: id (@PathVariable Long), form (@Valid FieldForm), bindingResult (BindingResult), model (Model), redirectAttributes (RedirectAttributes).
    // Output: String - Redirect to "/settings/garments/{id}" on success, or re-renders "settings/garment-template" on validation error.
    // @PostMapping("/{id}/fields") = Maps HTTP POST requests for "/settings/garments/{id}/fields".
    @PostMapping("/{id}/fields")
    public String addField(@PathVariable("id") Long id,
                           @Valid @ModelAttribute("fieldForm") FieldForm form,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("garment", garmentService.getGarmentById(id));
            model.addAttribute("fields", garmentService.getActiveFields(id));
            return "settings/garment-template";
        }

        try {
            garmentService.addField(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Field '" + form.getLabel() + "' added successfully.");
            return "redirect:/settings/garments/" + id;
        } catch (BusinessRuleException ex) {
            bindingResult.rejectValue("label", "duplicate", ex.getMessage());
            model.addAttribute("garment", garmentService.getGarmentById(id));
            model.addAttribute("fields", garmentService.getActiveFields(id));
            return "settings/garment-template";
        }
    }

    // Handles POST /settings/garments/{id}/fields/{fieldId}/rename: Renames a measurement field and updates its unit.
    // Inputs: id (@PathVariable Long), fieldId (@PathVariable Long), form (@Valid FieldForm), bindingResult, redirectAttributes.
    // Output: String - Redirect to "/settings/garments/{id}".
    // @PostMapping("/{id}/fields/{fieldId}/rename") = Maps HTTP POST requests for renaming a field.
    @PostMapping("/{id}/fields/{fieldId}/rename")
    public String renameField(@PathVariable("id") Long id,
                              @PathVariable("fieldId") Long fieldId,
                              @Valid @ModelAttribute("fieldForm") FieldForm form,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid field name or unit.");
            return "redirect:/settings/garments/" + id;
        }

        try {
            garmentService.renameField(id, fieldId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Field updated successfully.");
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/settings/garments/" + id;
    }

    // Handles POST /settings/garments/{id}/fields/{fieldId}/move-up: Swaps sort number with immediate upper neighbor.
    // Inputs: id (@PathVariable Long), fieldId (@PathVariable Long).
    // Output: String - Redirect to "/settings/garments/{id}".
    // @PostMapping("/{id}/fields/{fieldId}/move-up") = Maps HTTP POST requests to move a field up.
    @PostMapping("/{id}/fields/{fieldId}/move-up")
    public String moveFieldUp(@PathVariable("id") Long id, @PathVariable("fieldId") Long fieldId) {
        garmentService.moveFieldUp(id, fieldId);
        return "redirect:/settings/garments/" + id;
    }

    // Handles POST /settings/garments/{id}/fields/{fieldId}/move-down: Swaps sort number with immediate lower neighbor.
    // Inputs: id (@PathVariable Long), fieldId (@PathVariable Long).
    // Output: String - Redirect to "/settings/garments/{id}".
    // @PostMapping("/{id}/fields/{fieldId}/move-down") = Maps HTTP POST requests to move a field down.
    @PostMapping("/{id}/fields/{fieldId}/move-down")
    public String moveFieldDown(@PathVariable("id") Long id, @PathVariable("fieldId") Long fieldId) {
        garmentService.moveFieldDown(id, fieldId);
        return "redirect:/settings/garments/" + id;
    }

    // Handles POST /settings/garments/{id}/fields/{fieldId}/remove: Soft-deletes a measurement field (active = false).
    // Preserves old measurements in the measurement_value table.
    // Inputs: id (@PathVariable Long), fieldId (@PathVariable Long), redirectAttributes (RedirectAttributes).
    // Output: String - Redirect to "/settings/garments/{id}".
    // @PostMapping("/{id}/fields/{fieldId}/remove") = Maps HTTP POST requests to soft-delete a field.
    @PostMapping("/{id}/fields/{fieldId}/remove")
    public String removeField(@PathVariable("id") Long id,
                              @PathVariable("fieldId") Long fieldId,
                              RedirectAttributes redirectAttributes) {
        garmentService.removeField(id, fieldId);
        redirectAttributes.addFlashAttribute("successMessage", "Measurement field removed successfully.");
        return "redirect:/settings/garments/" + id;
    }
}
