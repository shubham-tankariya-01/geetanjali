package com.geetanjali.backend.service;

import com.geetanjali.backend.entity.Customer;
import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.entity.GarmentField;
import com.geetanjali.backend.exception.ResourceNotFoundException;
import com.geetanjali.backend.repository.CustomerOrderRepository;
import com.geetanjali.backend.repository.CustomerRepository;
import com.geetanjali.backend.repository.GarmentFieldRepository;
import com.geetanjali.backend.repository.GarmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerAndGarmentServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private GarmentRepository garmentRepository;

    @Mock
    private GarmentFieldRepository garmentFieldRepository;

    private Clock clock;
    private CustomerService customerService;
    private GarmentService garmentService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-10-13T10:00:00Z"), ZoneId.of("Asia/Kolkata"));
        customerService = new CustomerService(customerRepository, customerOrderRepository, clock);
        garmentService = new GarmentService(garmentRepository, garmentFieldRepository);
    }

    @Test
    void testGarmentService_listActiveGarments() {
        Garment shirt = new Garment("Shirt", 10, true);
        Garment pant = new Garment("Pant", 20, true);
        when(garmentRepository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(shirt, pant));

        List<Garment> garments = garmentService.listActiveGarments();
        assertEquals(2, garments.size());
        assertEquals("Shirt", garments.get(0).getName());
    }

    @Test
    void testGarmentService_getGarmentById_notFound() {
        when(garmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> garmentService.getGarmentById(99L));
    }

    @Test
    void testGarmentService_getActiveFields() {
        Garment shirt = new Garment("Shirt", 10, true);
        shirt.setId(1L);
        GarmentField chest = new GarmentField(shirt, "Chest", "inch", 1, true);

        when(garmentRepository.findById(1L)).thenReturn(Optional.of(shirt));
        when(garmentFieldRepository.findByGarmentIdAndActiveTrueOrderBySortOrderAsc(1L)).thenReturn(List.of(chest));

        List<GarmentField> fields = garmentService.getActiveFields(1L);
        assertEquals(1, fields.size());
        assertEquals("Chest", fields.get(0).getLabel());
    }

    @Test
    void testCustomerService_search_emptyQuery() {
        Customer c1 = new Customer("Anita", "9876543210");
        when(customerRepository.findAllByOrderByNameAsc()).thenReturn(List.of(c1));

        List<Customer> result = customerService.search("");
        assertEquals(1, result.size());
        assertEquals("Anita", result.get(0).getName());
    }

    @Test
    void testCustomerService_search_withQuery() {
        Customer c1 = new Customer("Anita", "9876543210");
        when(customerRepository.searchByNameOrMobile("Anita")).thenReturn(List.of(c1));

        List<Customer> result = customerService.search("  Anita  ");
        assertEquals(1, result.size());
        assertEquals("Anita", result.get(0).getName());
    }

    @Test
    void testCustomerService_findByMobile_normalizesPhone() {
        Customer c1 = new Customer("Anita", "9876543210");
        when(customerRepository.findByMobile("9876543210")).thenReturn(Optional.of(c1));

        Optional<Customer> result = customerService.findByMobile("+91 98765-43210");
        assertTrue(result.isPresent());
        assertEquals("Anita", result.get().getName());
    }

    @Test
    void testCustomerService_getById_notFound() {
        when(customerRepository.findById(123L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.getById(123L));
    }

    @Test
    void testCustomerService_countEarlierOrders() {
        when(customerOrderRepository.countByCustomerId(5L)).thenReturn(3L);

        assertEquals(3L, customerService.countEarlierOrders(5L));
    }

    @Test
    void testCustomerService_findLastOrderDate() {
        CustomerOrder order = new CustomerOrder();
        order.setCreatedAt(Instant.parse("2026-10-12T15:30:00Z"));
        when(customerOrderRepository.findFirstByCustomerIdOrderByCreatedAtDesc(5L)).thenReturn(Optional.of(order));

        Optional<LocalDate> date = customerService.findLastOrderDate(5L);
        assertTrue(date.isPresent());
        // 2026-10-12T15:30:00Z + 5:30 IST = 2026-10-12T21:00:00 IST -> 2026-10-12
        assertEquals(LocalDate.of(2026, 10, 12), date.get());
    }
}
