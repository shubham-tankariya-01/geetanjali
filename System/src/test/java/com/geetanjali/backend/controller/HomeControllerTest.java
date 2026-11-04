package com.geetanjali.backend.controller;

import com.geetanjali.backend.dto.PendingPaymentRow;
import com.geetanjali.backend.dto.PendingPaymentsSummary;
import com.geetanjali.backend.entity.Customer;
import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.service.OrderService;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// HomeControllerTest: Unit tests verifying HomeController renders dashboard with recent orders and pending balance.
@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private PaymentService paymentService;

    private HomeController controller;

    @BeforeEach
    void setUp() {
        controller = new HomeController(orderService, paymentService);
    }

    @Test
    @DisplayName("showHomePage returns 'home/index' and attaches up to 20 recent orders and pending balance to model")
    void testShowHomePage_returnsHomeViewWithRecentOrders() {
        CustomerOrder order1 = new CustomerOrder(1001L, new Customer("Deepak Verma", "9876543211"), null, Instant.now());
        CustomerOrder order2 = new CustomerOrder(1002L, new Customer("Ramesh Patel", "9876543212"), null, Instant.now());
        List<CustomerOrder> expectedOrders = List.of(order2, order1);
        PendingPaymentRow row1 = new PendingPaymentRow(1001L, 1001L, "TS-2026-1001", "Deepak Verma", "9876543211", 1L, new BigDecimal("400.00"), LocalDate.of(2026, 11, 3), "due in 2 days");
        PendingPaymentsSummary summary = new PendingPaymentsSummary(new BigDecimal("9500.00"), List.of(row1));

        when(orderService.getRecentOrders()).thenReturn(expectedOrders);
        when(orderService.getActiveOrdersSortedByDeliveryDate()).thenReturn(expectedOrders);
        when(paymentService.getTotalPendingBalance()).thenReturn(new BigDecimal("9500.00"));
        when(paymentService.getPendingPaymentsSummary()).thenReturn(summary);

        Model model = new ConcurrentModel();
        String view = controller.showHomePage(model);

        assertEquals("home/index", view);
        assertEquals(expectedOrders, model.getAttribute("recentOrders"));
        assertEquals(expectedOrders, model.getAttribute("activeOrders"));
        assertEquals(new BigDecimal("9500.00"), model.getAttribute("pendingTotal"));
        assertEquals(true, model.getAttribute("hasPendingBalance"));
        assertEquals(List.of(row1), model.getAttribute("pendingRows"));
        verify(orderService).getRecentOrders();
        verify(orderService).getActiveOrdersSortedByDeliveryDate();
        verify(paymentService).getTotalPendingBalance();
        verify(paymentService).getPendingPaymentsSummary();
    }
}
