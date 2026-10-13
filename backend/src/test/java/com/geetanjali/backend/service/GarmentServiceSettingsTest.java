package com.geetanjali.backend.service;

import com.geetanjali.backend.dto.form.FieldForm;
import com.geetanjali.backend.dto.form.GarmentForm;
import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.entity.GarmentField;
import com.geetanjali.backend.exception.BusinessRuleException;
import com.geetanjali.backend.repository.GarmentFieldRepository;
import com.geetanjali.backend.repository.GarmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// GarmentServiceSettingsTest: Unit tests for GarmentService settings operations:
// addGarment, removeGarment, addField, renameField, moveFieldUp, moveFieldDown, removeField.
@ExtendWith(MockitoExtension.class)
class GarmentServiceSettingsTest {

    @Mock
    private GarmentRepository garmentRepository;

    @Mock
    private GarmentFieldRepository garmentFieldRepository;

    private GarmentService garmentService;

    @BeforeEach
    void setUp() {
        garmentService = new GarmentService(garmentRepository, garmentFieldRepository);
    }

    @Test
    @DisplayName("addGarment saves new category at the end of the sort order")
    void testAddGarment_success() {
        Garment shirt = new Garment("Shirt", 10, true);
        when(garmentRepository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(shirt));
        when(garmentRepository.findByNameIgnoreCase("Kurta")).thenReturn(Optional.empty());
        when(garmentRepository.save(any(Garment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GarmentForm form = new GarmentForm("Kurta");
        Garment result = garmentService.addGarment(form);

        assertNotNull(result);
        assertEquals("Kurta", result.getName());
        assertEquals(20, result.getSortOrder()); // 10 + 10
        assertTrue(result.getActive());
        verify(garmentRepository).save(any(Garment.class));
    }

    @Test
    @DisplayName("addGarment throws BusinessRuleException when category name is already active")
    void testAddGarment_duplicateActive_throwsException() {
        Garment existing = new Garment("Shirt", 10, true);
        when(garmentRepository.findByNameIgnoreCase("Shirt")).thenReturn(Optional.of(existing));

        GarmentForm form = new GarmentForm("Shirt");
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> garmentService.addGarment(form));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(garmentRepository, never()).save(any(Garment.class));
    }

    @Test
    @DisplayName("addGarment reactivates soft-deleted category and places it at end")
    void testAddGarment_reactivatesInactiveCategory() {
        Garment activeShirt = new Garment("Shirt", 10, true);
        Garment inactiveKurta = new Garment("Kurta", 5, false);

        when(garmentRepository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(activeShirt));
        when(garmentRepository.findByNameIgnoreCase("Kurta")).thenReturn(Optional.of(inactiveKurta));
        when(garmentRepository.save(any(Garment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GarmentForm form = new GarmentForm("Kurta");
        Garment result = garmentService.addGarment(form);

        assertTrue(result.getActive());
        assertEquals(20, result.getSortOrder());
        verify(garmentRepository).save(inactiveKurta);
    }

    @Test
    @DisplayName("removeGarment sets active=false for soft delete")
    void testRemoveGarment_softDeletes() {
        Garment shirt = new Garment("Shirt", 10, true);
        shirt.setId(1L);
        when(garmentRepository.findById(1L)).thenReturn(Optional.of(shirt));

        garmentService.removeGarment(1L);

        assertFalse(shirt.getActive());
        verify(garmentRepository).save(shirt);
    }

    @Test
    @DisplayName("addField saves new field at end of garment template")
    void testAddField_success() {
        Garment shirt = new Garment("Shirt", 10, true);
        shirt.setId(1L);
        GarmentField chest = new GarmentField(shirt, "Chest", "inch", 10, true);

        when(garmentRepository.findById(1L)).thenReturn(Optional.of(shirt));
        when(garmentFieldRepository.findByGarmentIdAndLabelIgnoreCase(1L, "Waist")).thenReturn(Optional.empty());
        when(garmentFieldRepository.findByGarmentIdAndActiveTrueOrderBySortOrderAsc(1L)).thenReturn(List.of(chest));
        when(garmentFieldRepository.save(any(GarmentField.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FieldForm form = new FieldForm("Waist", "inch");
        GarmentField result = garmentService.addField(1L, form);

        assertNotNull(result);
        assertEquals("Waist", result.getLabel());
        assertEquals("inch", result.getUnit());
        assertEquals(20, result.getSortOrder());
        assertTrue(result.getActive());
        verify(garmentFieldRepository).save(any(GarmentField.class));
    }

    @Test
    @DisplayName("addField rejects duplicate field label for the same garment")
    void testAddField_duplicateActive_throwsException() {
        Garment shirt = new Garment("Shirt", 10, true);
        shirt.setId(1L);
        GarmentField chest = new GarmentField(shirt, "Chest", "inch", 10, true);

        when(garmentRepository.findById(1L)).thenReturn(Optional.of(shirt));
        when(garmentFieldRepository.findByGarmentIdAndLabelIgnoreCase(1L, "Chest")).thenReturn(Optional.of(chest));

        FieldForm form = new FieldForm("Chest", "inch");
        assertThrows(BusinessRuleException.class, () -> garmentService.addField(1L, form));
        verify(garmentFieldRepository, never()).save(any(GarmentField.class));
    }

    @Test
    @DisplayName("renameField updates label and unit successfully")
    void testRenameField_success() {
        Garment shirt = new Garment("Shirt", 10, true);
        shirt.setId(1L);
        GarmentField chest = new GarmentField(shirt, "Chest", "inch", 10, true);
        chest.setId(100L);

        when(garmentRepository.findById(1L)).thenReturn(Optional.of(shirt));
        when(garmentFieldRepository.findByIdAndGarmentId(100L, 1L)).thenReturn(Optional.of(chest));
        when(garmentFieldRepository.findByGarmentIdAndLabelIgnoreCase(1L, "Chest Circumference")).thenReturn(Optional.empty());
        when(garmentFieldRepository.save(any(GarmentField.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FieldForm form = new FieldForm("Chest Circumference", "cm");
        GarmentField result = garmentService.renameField(1L, 100L, form);

        assertEquals("Chest Circumference", result.getLabel());
        assertEquals("cm", result.getUnit());
        verify(garmentFieldRepository).save(chest);
    }

    @Test
    @DisplayName("renameField rejects if another field in the same garment already has that label")
    void testRenameField_duplicateLabel_throwsException() {
        Garment shirt = new Garment("Shirt", 10, true);
        shirt.setId(1L);
        GarmentField chest = new GarmentField(shirt, "Chest", "inch", 10, true);
        chest.setId(100L);
        GarmentField waist = new GarmentField(shirt, "Waist", "inch", 20, true);
        waist.setId(200L);

        when(garmentRepository.findById(1L)).thenReturn(Optional.of(shirt));
        when(garmentFieldRepository.findByIdAndGarmentId(100L, 1L)).thenReturn(Optional.of(chest));
        when(garmentFieldRepository.findByGarmentIdAndLabelIgnoreCase(1L, "Waist")).thenReturn(Optional.of(waist));

        FieldForm form = new FieldForm("Waist", "inch");
        assertThrows(BusinessRuleException.class, () -> garmentService.renameField(1L, 100L, form));
        verify(garmentFieldRepository, never()).save(chest);
    }

    @Test
    @DisplayName("moveFieldUp swaps sort numbers with upper neighbour")
    void testMoveFieldUp_swapsWithPrevious() {
        Garment shirt = new Garment("Shirt", 10, true);
        shirt.setId(1L);

        GarmentField f1 = new GarmentField(shirt, "Chest", "inch", 10, true);
        f1.setId(101L);
        GarmentField f2 = new GarmentField(shirt, "Waist", "inch", 20, true);
        f2.setId(102L);

        List<GarmentField> fields = new ArrayList<>(List.of(f1, f2));

        when(garmentRepository.findById(1L)).thenReturn(Optional.of(shirt));
        when(garmentFieldRepository.findByGarmentIdAndActiveTrueOrderBySortOrderAsc(1L)).thenReturn(fields);

        // Move f2 (Waist) UP
        garmentService.moveFieldUp(1L, 102L);

        assertEquals(10, f2.getSortOrder());
        assertEquals(20, f1.getSortOrder());
        verify(garmentFieldRepository).save(f2);
        verify(garmentFieldRepository).save(f1);
    }

    @Test
    @DisplayName("moveFieldDown swaps sort numbers with lower neighbour")
    void testMoveFieldDown_swapsWithNext() {
        Garment shirt = new Garment("Shirt", 10, true);
        shirt.setId(1L);

        GarmentField f1 = new GarmentField(shirt, "Chest", "inch", 10, true);
        f1.setId(101L);
        GarmentField f2 = new GarmentField(shirt, "Waist", "inch", 20, true);
        f2.setId(102L);

        List<GarmentField> fields = new ArrayList<>(List.of(f1, f2));

        when(garmentRepository.findById(1L)).thenReturn(Optional.of(shirt));
        when(garmentFieldRepository.findByGarmentIdAndActiveTrueOrderBySortOrderAsc(1L)).thenReturn(fields);

        // Move f1 (Chest) DOWN
        garmentService.moveFieldDown(1L, 101L);

        assertEquals(20, f1.getSortOrder());
        assertEquals(10, f2.getSortOrder());
        verify(garmentFieldRepository).save(f1);
        verify(garmentFieldRepository).save(f2);
    }

    @Test
    @DisplayName("removeField sets active=false for soft delete")
    void testRemoveField_softDeletes() {
        Garment shirt = new Garment("Shirt", 10, true);
        shirt.setId(1L);
        GarmentField chest = new GarmentField(shirt, "Chest", "inch", 10, true);
        chest.setId(101L);

        when(garmentRepository.findById(1L)).thenReturn(Optional.of(shirt));
        when(garmentFieldRepository.findByIdAndGarmentId(101L, 1L)).thenReturn(Optional.of(chest));

        garmentService.removeField(1L, 101L);

        assertFalse(chest.getActive());
        verify(garmentFieldRepository).save(chest);
    }
}
