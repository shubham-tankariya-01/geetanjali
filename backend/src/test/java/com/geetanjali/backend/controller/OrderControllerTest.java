package com.geetanjali.backend.controller;

import com.geetanjali.backend.dto.form.ItemForm;
import com.geetanjali.backend.entity.Customer;
import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.entity.OrderItem;
import com.geetanjali.backend.exception.ResourceNotFoundException;
import com.geetanjali.backend.service.GarmentService;
import com.geetanjali.backend.service.OrderService;
import com.geetanjali.backend.service.OrderWizardService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// OrderControllerTest: Unit tests verifying OrderController request handling, edit views, and view routing.
@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private OrderWizardService orderWizardService;

    @Mock
    private GarmentService garmentService;

    @Mock
    private com.geetanjali.backend.service.PaymentService paymentService;

    private Clock fixedClock;
    private OrderController controller;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-10-13T10:00:00Z"), ZoneId.of("Asia/Kolkata"));
        controller = new OrderController(orderService, orderWizardService, garmentService, paymentService, fixedClock);
    }

    @Test
    @DisplayName("showOrderDetail returns 'orders/detail' and attaches customer order to model")
    void testShowOrderDetail_existingOrder_returnsDetailView() {
        CustomerOrder order = new CustomerOrder(1001L, new Customer("Suresh Raina", "9876512345"), null, Instant.now());
        order.setId(10L);
        when(orderService.getOrderOrThrow(10L)).thenReturn(order);

        Model model = new ConcurrentModel();
        String view = controller.showOrderDetail(10L, model);

        assertEquals("orders/detail", view);
        assertEquals(order, model.getAttribute("order"));
        verify(orderService).getOrderOrThrow(10L);
    }

    @Test
    @DisplayName("showOrderDetail propagates ResourceNotFoundException when order does not exist")
    void testShowOrderDetail_unknownOrder_throwsResourceNotFoundException() {
        when(orderService.getOrderOrThrow(999L))
                .thenThrow(new ResourceNotFoundException("Order not found with id: 999"));

        Model model = new ConcurrentModel();
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                controller.showOrderDetail(999L, model)
        );
        assertEquals("Order not found with id: 999", ex.getMessage());
        verify(orderService).getOrderOrThrow(999L);
    }

    @Test
    @DisplayName("startAddItemsToOrder starts wizard with existing order and redirects to /orders/new/garment")
    void testStartAddItemsToOrder_startsWizardAndRedirectsToGarmentStep() {
        CustomerOrder order = new CustomerOrder(1001L, new Customer("Deepak Verma", "9876599999"), null, Instant.now());
        order.setId(15L);
        when(orderService.getOrderOrThrow(15L)).thenReturn(order);

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.startAddItemsToOrder(15L, redirectAttributes);

        assertEquals("redirect:/orders/new/garment", view);
        verify(orderWizardService).startAppendToOrder(order);
    }

    @Test
    @DisplayName("showEditOrderItem loads saved values and returns orders/item-edit")
    void testShowEditOrderItem_loadsSavedValuesAndRendersEditForm() {
        CustomerOrder order = new CustomerOrder(1001L, new Customer("Deepak Verma", "9876599999"), null, Instant.now());
        order.setId(15L);
        Garment shirt = new Garment("Shirt", 1, true);
        shirt.setId(1L);
        OrderItem item = new OrderItem(order, shirt, null, "Double pocket", new BigDecimal("500.00"), LocalDate.of(2026, 10, 25));
        item.setId(25L);

        when(orderService.getOrderOrThrow(15L)).thenReturn(order);
        when(orderService.getOrderItemOrThrow(15L, 25L)).thenReturn(item);

        ItemForm form = new ItemForm(LocalDate.of(2026, 10, 25), new BigDecimal("500.00"));
        when(orderService.prepareItemEditForm(item)).thenReturn(form);
        when(garmentService.getActiveFields(1L)).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String view = controller.showEditOrderItem(15L, 25L, model);

        assertEquals("orders/item-edit", view);
        assertEquals(order, model.getAttribute("order"));
        assertEquals(item, model.getAttribute("item"));
        assertEquals(form, model.getAttribute("itemForm"));
        assertEquals("/orders/15/items/25", model.getAttribute("formAction"));
        assertEquals("/orders/15", model.getAttribute("cancelUrl"));
    }

    @Test
    @DisplayName("handleUpdateOrderItem saves updates and redirects to /orders/{orderId}")
    void testHandleUpdateOrderItem_validForm_updatesAndRedirects() {
        ItemForm form = new ItemForm(LocalDate.of(2026, 10, 25), new BigDecimal("550.00"));
        form.setNotes("Updated styling note");

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "itemForm");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        Model model = new ConcurrentModel();

        String view = controller.handleUpdateOrderItem(15L, 25L, form, bindingResult, model, redirectAttributes);

        assertEquals("redirect:/orders/15", view);
        verify(orderService).updateOrderItem(eq(15L), eq(25L), any(ItemForm.class));
    }

    @Test
    @DisplayName("addPayment saves payment and redirects to safe internal returnTo URL")
    void testAddPayment_validForm_redirectsToInternalReturnTo() {
        com.geetanjali.backend.dto.form.PaymentForm form = new com.geetanjali.backend.dto.form.PaymentForm(
                new BigDecimal("200.00"),
                com.geetanjali.backend.enums.PaymentMode.UPI,
                LocalDate.of(2026, 10, 13),
                "/orders/15"
        );
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "paymentForm");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.addPayment(15L, form, bindingResult, redirectAttributes);

        assertEquals("redirect:/orders/15", view);
        verify(paymentService).addPayment(eq(15L), eq(form));
        assertEquals("Payment recorded successfully.", redirectAttributes.getFlashAttributes().get("successMessage"));
    }

    @Test
    @DisplayName("addPayment blocks external open redirect and falls back to /orders/{orderId}")
    void testAddPayment_openRedirectAttempt_fallsBackToOrderPage() {
        com.geetanjali.backend.dto.form.PaymentForm form = new com.geetanjali.backend.dto.form.PaymentForm(
                new BigDecimal("200.00"),
                com.geetanjali.backend.enums.PaymentMode.CASH,
                LocalDate.of(2026, 10, 13),
                "https://malicious-phishing.com/steal"
        );
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "paymentForm");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.addPayment(15L, form, bindingResult, redirectAttributes);

        assertEquals("redirect:/orders/15", view);
        verify(paymentService).addPayment(eq(15L), eq(form));
    }

    @Test
    @DisplayName("addPayment blocks protocol-relative open redirect //evil.com")
    void testAddPayment_protocolRelativeRedirect_fallsBackToOrderPage() {
        com.geetanjali.backend.dto.form.PaymentForm form = new com.geetanjali.backend.dto.form.PaymentForm(
                new BigDecimal("200.00"),
                com.geetanjali.backend.enums.PaymentMode.CASH,
                LocalDate.of(2026, 10, 13),
                "//evil.com"
        );
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "paymentForm");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.addPayment(15L, form, bindingResult, redirectAttributes);

        assertEquals("redirect:/orders/15", view);
    }

    @Test
    @DisplayName("addPayment catches BusinessRuleException and flashes errorMessage")
    void testAddPayment_businessRuleException_flashesErrorMessage() {
        com.geetanjali.backend.dto.form.PaymentForm form = new com.geetanjali.backend.dto.form.PaymentForm(
                new BigDecimal("9999.00"),
                com.geetanjali.backend.enums.PaymentMode.CASH,
                LocalDate.of(2026, 10, 13),
                "/orders/15"
        );
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "paymentForm");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        when(paymentService.addPayment(eq(15L), any())).thenThrow(new com.geetanjali.backend.exception.BusinessRuleException("Exceeds remaining balance"));

        String view = controller.addPayment(15L, form, bindingResult, redirectAttributes);

        assertEquals("redirect:/orders/15", view);
        assertEquals("Exceeds remaining balance", redirectAttributes.getFlashAttributes().get("errorMessage"));
    }
}
