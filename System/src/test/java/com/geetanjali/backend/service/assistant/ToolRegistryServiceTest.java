package com.geetanjali.backend.service.assistant;

import com.geetanjali.backend.dto.PendingPaymentRow;
import com.geetanjali.backend.dto.PendingPaymentsSummary;
import com.geetanjali.backend.entity.*;
import com.geetanjali.backend.enums.ItemStatus;
import com.geetanjali.backend.enums.Role;
import com.geetanjali.backend.repository.CustomerOrderRepository;
import com.geetanjali.backend.repository.CustomerRepository;
import com.geetanjali.backend.repository.OrderItemRepository;
import com.geetanjali.backend.repository.PaymentRepository;
import com.geetanjali.backend.service.PaymentService;
import com.geetanjali.backend.service.assistant.model.ToolCall;
import com.geetanjali.backend.service.assistant.model.ToolResult;
import com.geetanjali.backend.util.OrderNumberFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolRegistryServiceTest {

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private com.geetanjali.backend.service.MeasurementService measurementService;

    @Mock
    private com.geetanjali.backend.repository.AppUserRepository appUserRepository;

    @Mock
    private OrderNumberFormatter orderNumberFormatter;

    private Clock fixedClock;
    private ToolRegistryService toolRegistryService;

    private Shop shop;
    private AppUser ownerUser;
    private AppUser tailorUser;
    private Customer customer;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-11-04T10:00:00Z"), ZoneId.of("Asia/Kolkata"));
        toolRegistryService = new ToolRegistryService(
                customerOrderRepository,
                orderItemRepository,
                customerRepository,
                paymentRepository,
                paymentService,
                measurementService,
                appUserRepository,
                orderNumberFormatter,
                fixedClock
        );

        shop = new Shop("Geetanjali Studio");
        shop.setId(1L);

        ownerUser = new AppUser(shop, "Shubham Owner", "9876543210", "hash", Role.OWNER);
        tailorUser = new AppUser(shop, "Ketan Tailor", "9876543211", "hash", Role.TAILOR);

        customer = new Customer("Ramesh Patel", "9876500000");
        customer.setId(10L);
    }

    @Test
    @DisplayName("getOrderStatus returns order details with items, total, and balance")
    void testGetOrderStatus_success() {
        CustomerOrder order = new CustomerOrder(1001L, customer, ownerUser, Instant.now());
        order.setId(101L);

        Garment shirt = new Garment("Shirt", 1, true);
        OrderItem item = new OrderItem(order, shirt, null, "Double stitch", new BigDecimal("1200.00"), LocalDate.of(2026, 11, 10));
        item.setStatus(ItemStatus.PENDING);
        order.setItems(List.of(item));

        when(customerOrderRepository.findByOrderNo(1001L)).thenReturn(Optional.of(order));
        when(orderNumberFormatter.format(order)).thenReturn("TS-2026-1001");

        ToolCall call = new ToolCall("call_1", "getOrderStatus", Map.of("orderIdentifier", "1001"));
        ToolResult result = toolRegistryService.executeTool(1L, Role.TAILOR, call);

        assertThat(result.isSuccess()).isTrue();
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertThat(data.get("found")).isEqualTo(true);
        assertThat(data.get("formattedOrderNo")).isEqualTo("TS-2026-1001");
        assertThat(data.get("customerName")).isEqualTo("Ramesh Patel");
        assertThat(data.get("totalAmount")).isEqualTo("1200.00");
    }

    @Test
    @DisplayName("getRevenueMetrics succeeds for OWNER role")
    void testGetRevenueMetrics_ownerAllowed() {
        CustomerOrder order = new CustomerOrder(1001L, customer, ownerUser, Instant.now());
        Payment payment = new Payment(order, new BigDecimal("1500.00"), "CASH", LocalDate.of(2026, 11, 2));

        when(paymentRepository.findAll()).thenReturn(List.of(payment));

        ToolCall call = new ToolCall("call_2", "getRevenueMetrics", Map.of("period", "THIS_MONTH"));
        ToolResult result = toolRegistryService.executeTool(1L, Role.OWNER, call);

        assertThat(result.isSuccess()).isTrue();
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertThat(data.get("totalCollectedRevenue")).isEqualTo("Rs. 1500.00");
        assertThat(data.get("paymentTransactionsCount")).isEqualTo(1);
    }

    @Test
    @DisplayName("getRevenueMetrics is blocked for TAILOR role")
    void testGetRevenueMetrics_tailorBlocked() {
        ToolCall call = new ToolCall("call_3", "getRevenueMetrics", Map.of("period", "THIS_MONTH"));
        ToolResult result = toolRegistryService.executeTool(1L, Role.TAILOR, call);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Access Restricted");
    }

    @Test
    @DisplayName("getUnpaidOrders succeeds for OWNER and blocks TAILOR")
    void testGetUnpaidOrders_rbacEnforced() {
        // 1. Owner role check
        PendingPaymentRow row = new PendingPaymentRow(101L, 1001L, "TS-2026-1001", "Ramesh", "9876500000", 10L, new BigDecimal("500.00"), LocalDate.of(2026, 11, 10), "due in 6 days");
        PendingPaymentsSummary summary = new PendingPaymentsSummary(new BigDecimal("500.00"), List.of(row));
        when(paymentService.getPendingPaymentsSummary()).thenReturn(summary);

        ToolCall call = new ToolCall("call_4", "getUnpaidOrders", Map.of());
        ToolResult ownerResult = toolRegistryService.executeTool(1L, Role.OWNER, call);
        assertThat(ownerResult.isSuccess()).isTrue();

        // 2. Tailor role check
        ToolResult tailorResult = toolRegistryService.executeTool(1L, Role.TAILOR, call);
        assertThat(tailorResult.isSuccess()).isFalse();
        assertThat(tailorResult.getErrorMessage()).contains("Access Restricted");
    }

    @Test
    @DisplayName("getCustomerSummary searches by name or phone")
    void testGetCustomerSummary_success() {
        when(customerRepository.searchByNameOrMobile("Ramesh")).thenReturn(List.of(customer));
        when(customerOrderRepository.findByCustomerIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());

        ToolCall call = new ToolCall("call_5", "getCustomerSummary", Map.of("query", "Ramesh"));
        ToolResult result = toolRegistryService.executeTool(1L, Role.TAILOR, call);

        assertThat(result.isSuccess()).isTrue();
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertThat(data.get("found")).isEqualTo(true);
        List<?> customersList = (List<?>) data.get("customers");
        assertThat(customersList).hasSize(1);
    }

    @Test
    @DisplayName("getCustomerMeasurements retrieves garment measurements for customer")
    void testGetCustomerMeasurements_success() {
        when(customerRepository.searchByNameOrMobile("Ramesh")).thenReturn(List.of(customer));

        Garment shirt = new Garment("Shirt", 1, true);
        GarmentField chestField = new GarmentField(shirt, "Chest", "inch", 1, true);
        Measurement measurement = new Measurement(customer, shirt, Instant.now());
        MeasurementValue mv = new MeasurementValue();
        mv.setMeasurement(measurement);
        mv.setField(chestField);
        mv.setValue(new BigDecimal("38.50"));

        com.geetanjali.backend.dto.response.GarmentLatestMeasurement glm =
                new com.geetanjali.backend.dto.response.GarmentLatestMeasurement(shirt, measurement, List.of(mv));
        when(measurementService.getLatestPerGarment(customer)).thenReturn(List.of(glm));

        ToolCall call = new ToolCall("call_6", "getCustomerMeasurements", Map.of("customerQuery", "Ramesh"));
        ToolResult result = toolRegistryService.executeTool(1L, Role.TAILOR, call);

        assertThat(result.isSuccess()).isTrue();
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertThat(data.get("found")).isEqualTo(true);
        List<Map<String, Object>> customers = (List<Map<String, Object>>) data.get("customers");
        assertThat(customers).hasSize(1);
        List<Map<String, Object>> measurements = (List<Map<String, Object>>) customers.get(0).get("measurements");
        assertThat(measurements).hasSize(1);
        assertThat(measurements.get(0).get("garmentName")).isEqualTo("Shirt");
        Map<String, String> sizes = (Map<String, String>) measurements.get(0).get("sizes");
        assertThat(sizes.get("Chest")).isEqualTo("38.50 inch");
    }

    @Test
    @DisplayName("getStaffOrders returns orders and active count for staff member")
    void testGetStaffOrders_success() {
        when(appUserRepository.findByMobileOrName("Ketan")).thenReturn(List.of(tailorUser));
        tailorUser.setId(2L);

        CustomerOrder order = new CustomerOrder(1002L, customer, tailorUser, Instant.now());
        order.setId(102L);
        when(customerOrderRepository.findByCreatedByIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(order));
        when(orderNumberFormatter.format(order)).thenReturn("TS-2026-1002");

        ToolCall call = new ToolCall("call_7", "getStaffOrders", Map.of("staffQuery", "Ketan"));
        ToolResult result = toolRegistryService.executeTool(1L, Role.TAILOR, call);

        assertThat(result.isSuccess()).isTrue();
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertThat(data.get("found")).isEqualTo(true);
        List<Map<String, Object>> staffList = (List<Map<String, Object>>) data.get("staffMembers");
        assertThat(staffList).hasSize(1);
        assertThat(staffList.get(0).get("staffName")).isEqualTo("Ketan Tailor");
        assertThat(staffList.get(0).get("totalOrdersBooked")).isEqualTo(1);
        assertThat(staffList.get(0).get("activeOrdersCount")).isEqualTo(1L);
    }
}
