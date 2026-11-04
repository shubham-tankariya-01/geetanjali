package com.geetanjali.backend.service;

import com.geetanjali.backend.dto.draft.OrderDraft;
import com.geetanjali.backend.dto.form.CustomerStepForm;
import com.geetanjali.backend.dto.form.ItemForm;
import com.geetanjali.backend.dto.draft.OrderDraftItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import com.geetanjali.backend.entity.Customer;
import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.exception.ResourceNotFoundException;
import com.geetanjali.backend.repository.CustomerRepository;
import com.geetanjali.backend.repository.GarmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// OrderWizardTest: Unit tests verifying OrderDraft session state and OrderWizardService logic.
@ExtendWith(MockitoExtension.class)
class OrderWizardTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private GarmentRepository garmentRepository;

    private OrderDraft orderDraft;
    private OrderWizardService orderWizardService;

    @BeforeEach
    void setUp() {
        orderDraft = new OrderDraft();
        orderWizardService = new OrderWizardService(orderDraft, customerRepository, garmentRepository);
    }

    @Test
    @DisplayName("startFresh resets the draft completely")
    void testStartFresh_resetsDraft() {
        orderDraft.setCustomer(10L, "Old Customer", "9876543210");
        orderDraft.setCurrentGarment(new Garment("Shirt", 1, true));

        orderWizardService.startFresh();

        assertNull(orderDraft.getCustomerId());
        assertNull(orderDraft.getCustomerName());
        assertNull(orderDraft.getCustomerMobile());
        assertNull(orderDraft.getCurrentGarment());
        assertFalse(orderDraft.hasCustomer());
    }

    @Test
    @DisplayName("startForCustomer loads existing customer and sets details on draft")
    void testStartForCustomer_loadsCustomerDetails() {
        Customer customer = new Customer("Ramesh Patel", "9876543210");
        setEntityId(customer, 42L);

        when(customerRepository.findById(42L)).thenReturn(Optional.of(customer));

        orderWizardService.startForCustomer(42L);

        assertEquals(42L, orderDraft.getCustomerId());
        assertEquals("Ramesh Patel", orderDraft.getCustomerName());
        assertEquals("9876543210", orderDraft.getCustomerMobile());
        assertTrue(orderDraft.hasCustomer());
        assertFalse(orderDraft.isNewCustomer());
    }

    @Test
    @DisplayName("setCustomer links to existing customer when mobile matches DB record")
    void testSetCustomer_linksExistingCustomerByMobile() {
        Customer existing = new Customer("Suresh Sharma", "9123456789");
        setEntityId(existing, 88L);

        when(customerRepository.findByMobile("9123456789")).thenReturn(Optional.of(existing));

        CustomerStepForm form = new CustomerStepForm(null, "+91 91234-56789", "Suresh");
        orderWizardService.setCustomer(form);

        assertEquals(88L, orderDraft.getCustomerId());
        assertEquals("Suresh Sharma", orderDraft.getCustomerName());
        assertEquals("9123456789", orderDraft.getCustomerMobile());
        assertFalse(orderDraft.isNewCustomer());
    }

    @Test
    @DisplayName("setCustomer stores new customer info in draft when mobile not found in DB")
    void testSetCustomer_storesNewCustomerDetailsInDraft() {
        when(customerRepository.findByMobile("9988776655")).thenReturn(Optional.empty());

        CustomerStepForm form = new CustomerStepForm(null, "9988776655", "Pooja Mehta");
        orderWizardService.setCustomer(form);

        assertNull(orderDraft.getCustomerId());
        assertEquals("Pooja Mehta", orderDraft.getCustomerName());
        assertEquals("9988776655", orderDraft.getCustomerMobile());
        assertTrue(orderDraft.hasCustomer());
        assertTrue(orderDraft.isNewCustomer());
    }

    @Test
    @DisplayName("selectGarment verifies garment and initializes draft currentGarment and currentItem")
    void testSelectGarment_setsCurrentGarmentAndItem() {
        Garment shirt = new Garment("Shirt", 1, true);
        setEntityId(shirt, 5L);

        when(garmentRepository.findById(5L)).thenReturn(Optional.of(shirt));

        Garment selected = orderWizardService.selectGarment(5L);

        assertEquals("Shirt", selected.getName());
        assertEquals(shirt, orderDraft.getCurrentGarment());
        assertNotNull(orderDraft.getCurrentItem());
        assertEquals(shirt, orderDraft.getCurrentItem().getGarment());
    }

    @Test
    @DisplayName("selectGarment throws ResourceNotFoundException when garment ID does not exist")
    void testSelectGarment_throwsWhenMissing() {
        when(garmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderWizardService.selectGarment(999L));
    }

    @Test
    @DisplayName("saveItem copies values into new OrderDraftItem, adds to draft, and clears working garment")
    void testSaveItem_copiesValuesAndAppendsToDraft() {
        Garment shirt = new Garment("Shirt", 1, true);
        setEntityId(shirt, 1L);
        orderDraft.setCurrentGarment(shirt);

        ItemForm form = new ItemForm(LocalDate.of(2026, 10, 20), new BigDecimal("450.00"));
        form.setNotes("Double stitching, mandarin collar");
        form.setMeasurements(Map.of(101L, new BigDecimal("30.0"), 102L, new BigDecimal("38.5")));

        OrderDraftItem savedItem = orderWizardService.saveItem(form);

        assertNotNull(savedItem);
        assertEquals(shirt, savedItem.getGarment());
        assertEquals("Double stitching, mandarin collar", savedItem.getNotes());
        assertEquals(new BigDecimal("450.00"), savedItem.getPrice());
        assertEquals(2, savedItem.getMeasurementValues().size());

        // Draft assertions
        assertEquals(1, orderDraft.getItemCount());
        assertEquals(new BigDecimal("450.00"), orderDraft.getTotalPrice());
        assertNull(orderDraft.getCurrentGarment(), "Working garment should be cleared after save");
    }

    @Test
    @DisplayName("Adding two items leaves two items in the draft with combined total price")
    void testSaveMultipleItems_retainsAllItemsInDraft() {
        Garment shirt = new Garment("Shirt", 1, true);
        Garment pant = new Garment("Pant", 2, true);
        setEntityId(shirt, 1L);
        setEntityId(pant, 2L);

        // Item 1: Shirt
        orderDraft.setCurrentGarment(shirt);
        ItemForm shirtForm = new ItemForm(LocalDate.of(2026, 10, 20), new BigDecimal("400.00"));
        orderWizardService.saveItem(shirtForm);

        // Item 2: Pant
        orderDraft.setCurrentGarment(pant);
        ItemForm pantForm = new ItemForm(LocalDate.of(2026, 10, 22), new BigDecimal("550.00"));
        orderWizardService.saveItem(pantForm);

        // Assert: 2 items in cart
        assertEquals(2, orderDraft.getItemCount(), "Adding two items must leave two items in the draft");
        assertEquals("Shirt", orderDraft.getItems().get(0).getGarment().getName());
        assertEquals("Pant", orderDraft.getItems().get(1).getGarment().getName());
        assertEquals(new BigDecimal("950.00"), orderDraft.getTotalPrice(), "Total price must equal 400 + 550");
    }

    @Test
    @DisplayName("saveItem throws IllegalStateException if called without a selected garment")
    void testSaveItem_throwsWhenNoGarmentSelected() {
        orderDraft.setCurrentGarment(null);
        ItemForm form = new ItemForm(LocalDate.of(2026, 10, 20), new BigDecimal("300.00"));

        assertThrows(IllegalStateException.class, () -> orderWizardService.saveItem(form));
    }

    private void setEntityId(Object entity, Long id) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
