package com.geetanjali.backend.controller;

import com.geetanjali.backend.dto.PendingPaymentRow;
import com.geetanjali.backend.dto.PendingPaymentsSummary;
import com.geetanjali.backend.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// PaymentControllerTest: Controller unit tests verifying HTTP GET /payments/pending.
@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    private PaymentController controller;

    @BeforeEach
    void setUp() {
        controller = new PaymentController(paymentService);
    }

    @Test
    @DisplayName("showPendingPayments returns 'payments/pending' view and attaches totalOutstanding and pendingRows to model")
    void testShowPendingPayments_rendersViewAndPopulatesModel() {
        PendingPaymentRow row = new PendingPaymentRow(
                10L,
                1001L,
                "TS-2026-1001",
                "Ramesh Patel",
                "9876543210",
                1L,
                new BigDecimal("800.00"),
                LocalDate.of(2026, 10, 20),
                "due in 5 days"
        );
        PendingPaymentsSummary summary = new PendingPaymentsSummary(
                new BigDecimal("800.00"),
                List.of(row)
        );

        when(paymentService.getPendingPaymentsSummary()).thenReturn(summary);

        Model model = new ConcurrentModel();
        String view = controller.showPendingPayments(model);

        assertEquals("payments/pending", view);
        assertEquals(new BigDecimal("800.00"), model.getAttribute("totalOutstanding"));
        assertEquals(List.of(row), model.getAttribute("pendingRows"));
        verify(paymentService).getPendingPaymentsSummary();
    }
}
