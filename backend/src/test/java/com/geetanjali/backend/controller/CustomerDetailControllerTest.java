package com.geetanjali.backend.controller;

import com.geetanjali.backend.dto.response.GarmentLatestMeasurement;
import com.geetanjali.backend.entity.Customer;
import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.service.CustomerService;
import com.geetanjali.backend.service.MeasurementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// CustomerDetailControllerTest: Unit test for CustomerController detail page with order history and preselection.
@ExtendWith(MockitoExtension.class)
class CustomerDetailControllerTest {

    @Mock
    private CustomerService customerService;

    @Mock
    private MeasurementService measurementService;

    private CustomerController customerController;

    @BeforeEach
    void setUp() {
        customerController = new CustomerController(customerService, measurementService);
    }

    @Test
    @DisplayName("showCustomerDetail populates customer, order history, measurements, and stats")
    void testShowCustomerDetail_populatesOrdersAndMeasurements() {
        Customer customer = new Customer("Ramesh Patel", "9876543210");
        customer.setId(10L);

        CustomerOrder order1 = new CustomerOrder();
        order1.setId(100L);
        order1.setOrderNo(1001L);

        Garment shirt = new Garment("Shirt", 1, true);
        GarmentLatestMeasurement glm = new GarmentLatestMeasurement(shirt, null, List.of());

        when(customerService.getById(10L)).thenReturn(customer);
        when(customerService.countEarlierOrders(10L)).thenReturn(1L);
        when(customerService.findLastOrderDate(10L)).thenReturn(Optional.of(LocalDate.of(2026, 10, 12)));
        when(customerService.getCustomerOrders(10L)).thenReturn(List.of(order1));
        when(measurementService.getLatestPerGarment(customer)).thenReturn(List.of(glm));

        Model model = new ConcurrentModel();
        String view = customerController.showCustomerDetail(10L, model);

        assertEquals("customers/detail", view);
        assertEquals(customer, model.getAttribute("customer"));
        assertEquals(1L, model.getAttribute("orderCount"));
        assertEquals(LocalDate.of(2026, 10, 12), model.getAttribute("lastOrderDate"));
        assertEquals(List.of(order1), model.getAttribute("orders"));
        assertEquals(List.of(glm), model.getAttribute("garmentMeasurements"));
    }
}
