package com.geetanjali.backend.controller;

import com.geetanjali.backend.dto.draft.OrderDraft;
import com.geetanjali.backend.service.*;
import com.geetanjali.backend.service.storage.PhotoStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

// OrderWizardCustomerPreselectTest: Tests the customer preselection and direct jump to Step 2.
@ExtendWith(MockitoExtension.class)
class OrderWizardCustomerPreselectTest {

    @Mock
    private OrderWizardService orderWizardService;
    @Mock
    private GarmentService garmentService;
    @Mock
    private MeasurementService measurementService;
    @Mock
    private OrderService orderService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private PhotoStorageService photoStorageService;

    private OrderWizardController controller;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-13T10:00:00Z"), ZoneId.of("Asia/Kolkata"));
        controller = new OrderWizardController(
                orderWizardService,
                garmentService,
                measurementService,
                orderService,
                paymentService,
                photoStorageService,
                clock
        );
    }

    @Test
    @DisplayName("showCustomerStep with customerId preselects customer and jumps straight to Step 2")
    void testShowCustomerStep_withCustomerId_jumpsToStep2() {
        Model model = new ConcurrentModel();

        String view = controller.showCustomerStep(42L, model);

        verify(orderWizardService).startForCustomer(42L);
        assertEquals("redirect:/orders/new/garment", view);
    }

    @Test
    @DisplayName("showCustomerStep without customerId resets draft and shows Step 1")
    void testShowCustomerStep_withoutCustomerId_showsStep1() {
        Model model = new ConcurrentModel();
        when(orderWizardService.getDraft()).thenReturn(new OrderDraft());

        String view = controller.showCustomerStep(null, model);

        verify(orderWizardService).startFresh();
        assertEquals("orders/wizard-customer", view);
    }
}
