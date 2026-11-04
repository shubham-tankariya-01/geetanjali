package com.geetanjali.backend.controller;

import com.geetanjali.backend.dto.form.FieldForm;
import com.geetanjali.backend.dto.form.GarmentForm;
import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.entity.GarmentField;
import com.geetanjali.backend.exception.BusinessRuleException;
import com.geetanjali.backend.service.GarmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// GarmentControllerTest: Unit tests for GarmentController endpoints.
@ExtendWith(MockitoExtension.class)
class GarmentControllerTest {

    @Mock
    private GarmentService garmentService;

    private GarmentController garmentController;

    @BeforeEach
    void setUp() {
        garmentController = new GarmentController(garmentService);
    }

    @Test
    @DisplayName("showGarments loads active categories and returns settings/garments view")
    void testShowGarments() {
        Garment shirt = new Garment("Shirt", 1, true);
        when(garmentService.listActiveGarments()).thenReturn(List.of(shirt));

        Model model = new ConcurrentModel();
        String view = garmentController.showGarments(model);

        assertEquals("settings/garments", view);
        assertTrue(model.containsAttribute("garments"));
        assertTrue(model.containsAttribute("garmentForm"));
    }

    @Test
    @DisplayName("addGarment successfully creates category and redirects")
    void testAddGarment_success() {
        GarmentForm form = new GarmentForm("Kurta");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "garmentForm");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        Model model = new ConcurrentModel();

        Garment created = new Garment("Kurta", 10, true);
        when(garmentService.addGarment(any(GarmentForm.class))).thenReturn(created);

        String view = garmentController.addGarment(form, bindingResult, model, redirectAttributes);

        assertEquals("redirect:/settings/garments", view);
        assertEquals("Garment category 'Kurta' added successfully.", redirectAttributes.getFlashAttributes().get("successMessage"));
    }

    @Test
    @DisplayName("addGarment rejects duplicate category with form error")
    void testAddGarment_duplicateRejection() {
        GarmentForm form = new GarmentForm("Shirt");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "garmentForm");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        Model model = new ConcurrentModel();

        when(garmentService.addGarment(any(GarmentForm.class)))
                .thenThrow(new BusinessRuleException("A garment category named 'Shirt' already exists."));
        when(garmentService.listActiveGarments()).thenReturn(List.of(new Garment("Shirt", 1, true)));

        String view = garmentController.addGarment(form, bindingResult, model, redirectAttributes);

        assertEquals("settings/garments", view);
        assertTrue(bindingResult.hasFieldErrors("name"));
    }

    @Test
    @DisplayName("removeGarment calls service and redirects to garments list")
    void testRemoveGarment() {
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = garmentController.removeGarment(5L, redirectAttributes);

        verify(garmentService).removeGarment(5L);
        assertEquals("redirect:/settings/garments", view);
        assertEquals("Garment category removed successfully.", redirectAttributes.getFlashAttributes().get("successMessage"));
    }

    @Test
    @DisplayName("showGarmentTemplate loads category and fields and returns settings/garment-template view")
    void testShowGarmentTemplate() {
        Garment shirt = new Garment("Shirt", 1, true);
        shirt.setId(1L);
        GarmentField chest = new GarmentField(shirt, "Chest", "inch", 1, true);

        when(garmentService.getGarmentById(1L)).thenReturn(shirt);
        when(garmentService.getActiveFields(1L)).thenReturn(List.of(chest));

        Model model = new ConcurrentModel();
        String view = garmentController.showGarmentTemplate(1L, model);

        assertEquals("settings/garment-template", view);
        assertEquals(shirt, model.getAttribute("garment"));
        assertEquals(List.of(chest), model.getAttribute("fields"));
        assertTrue(model.containsAttribute("fieldForm"));
    }

    @Test
    @DisplayName("addField adds field and redirects to garment template")
    void testAddField_success() {
        FieldForm form = new FieldForm("Length", "inch");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "fieldForm");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        Model model = new ConcurrentModel();

        String view = garmentController.addField(1L, form, bindingResult, model, redirectAttributes);

        verify(garmentService).addField(1L, form);
        assertEquals("redirect:/settings/garments/1", view);
        assertEquals("Field 'Length' added successfully.", redirectAttributes.getFlashAttributes().get("successMessage"));
    }

    @Test
    @DisplayName("renameField updates field and redirects")
    void testRenameField_success() {
        FieldForm form = new FieldForm("Waist Circumference", "cm");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "fieldForm");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = garmentController.renameField(1L, 10L, form, bindingResult, redirectAttributes);

        verify(garmentService).renameField(1L, 10L, form);
        assertEquals("redirect:/settings/garments/1", view);
        assertEquals("Field updated successfully.", redirectAttributes.getFlashAttributes().get("successMessage"));
    }

    @Test
    @DisplayName("moveFieldUp and moveFieldDown call service and redirect")
    void testMoveFieldUpDown() {
        String upView = garmentController.moveFieldUp(1L, 10L);
        verify(garmentService).moveFieldUp(1L, 10L);
        assertEquals("redirect:/settings/garments/1", upView);

        String downView = garmentController.moveFieldDown(1L, 10L);
        verify(garmentService).moveFieldDown(1L, 10L);
        assertEquals("redirect:/settings/garments/1", downView);
    }

    @Test
    @DisplayName("removeField calls service and redirects with flash message")
    void testRemoveField() {
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = garmentController.removeField(1L, 10L, redirectAttributes);

        verify(garmentService).removeField(1L, 10L);
        assertEquals("redirect:/settings/garments/1", view);
        assertEquals("Measurement field removed successfully.", redirectAttributes.getFlashAttributes().get("successMessage"));
    }
}
