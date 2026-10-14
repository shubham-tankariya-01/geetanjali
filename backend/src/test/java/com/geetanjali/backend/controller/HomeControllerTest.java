package com.geetanjali.backend.controller;

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

        when(orderService.getRecentOrders()).thenReturn(expectedOrders);
        when(paymentService.getTotalPendingBalance()).thenReturn(new BigDecimal("9500.00"));

        Model model = new ConcurrentModel();
        String view = controller.showHomePage(model);

        assertEquals("home/index", view);
        assertEquals(expectedOrders, model.getAttribute("recentOrders"));
        assertEquals(new BigDecimal("9500.00"), model.getAttribute("pendingTotal"));
        assertEquals(true, model.getAttribute("hasPendingBalance"));
        verify(orderService).getRecentOrders();
        verify(paymentService).getTotalPendingBalance();
    }
}
