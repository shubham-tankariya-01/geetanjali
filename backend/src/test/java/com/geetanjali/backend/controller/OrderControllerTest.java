package com.geetanjali.backend.controller;

import com.geetanjali.backend.entity.Customer;
import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.exception.ResourceNotFoundException;
import com.geetanjali.backend.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// OrderControllerTest: Unit tests verifying OrderController request handling and view routing.
@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    private OrderController controller;

    @BeforeEach
    void setUp() {
        controller = new OrderController(orderService);
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
}
