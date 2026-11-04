package com.geetanjali.backend.service;

import com.geetanjali.backend.dto.draft.OrderDraft;
import com.geetanjali.backend.dto.draft.OrderDraftItem;
import com.geetanjali.backend.dto.form.AdvanceForm;
import com.geetanjali.backend.entity.AppUser;
import com.geetanjali.backend.entity.Customer;
import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.entity.OrderItem;
import com.geetanjali.backend.entity.Payment;
import com.geetanjali.backend.enums.PaymentMode;
import com.geetanjali.backend.repository.AppUserRepository;
import com.geetanjali.backend.repository.CustomerOrderRepository;
import com.geetanjali.backend.repository.CustomerRepository;
import com.geetanjali.backend.repository.GarmentRepository;
import com.geetanjali.backend.repository.OrderItemRepository;
import com.geetanjali.backend.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// OrderTransactionRollbackIntegrationTest: Verifies atomic order placement and transaction rollback.
// Demonstrates that if an unexpected RuntimeException occurs midway through saving an order,
// Spring's @Transactional completely rolls back all changes, leaving zero orphaned rows in PostgreSQL!
@SpringBootTest
@Import(OrderTransactionRollbackIntegrationTest.TransactionTestHelper.class)
class OrderTransactionRollbackIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private GarmentRepository garmentRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private Clock clock;

    @Autowired
    private TransactionTestHelper transactionTestHelper;

    private AppUser testUser;
    private Garment shirtGarment;

    @BeforeEach
    void setUp() {
        testUser = appUserRepository.findAll().stream().findFirst().orElse(null);
        shirtGarment = garmentRepository.findAll().stream().findFirst().orElse(null);
    }

    @Test
    void testPlaceOrder_happyPath_persistsAcrossAllTables() {
        assertNotNull(testUser, "Test requires at least one app_user in DB");
        assertNotNull(shirtGarment, "Test requires at least one garment in DB");

        OrderDraft draft = new OrderDraft();
        draft.setCustomer(null, "Atomic Test Customer", "9900112233");

        OrderDraftItem item = new OrderDraftItem(
                shirtGarment,
                Map.of(1L, new BigDecimal("38.0")),
                "Transaction test item",
                LocalDate.now(clock).plusDays(5),
                new BigDecimal("650.00"),
                List.of()
        );
        draft.addItem(item);

        AdvanceForm advanceForm = new AdvanceForm(new BigDecimal("300.00"), PaymentMode.CASH);

        // Place order
        CustomerOrder savedOrder = orderService.placeOrder(draft, advanceForm, testUser);

        assertNotNull(savedOrder);
        assertNotNull(savedOrder.getId());
        assertTrue(savedOrder.getOrderNo() >= 1001L);

        // Verify rows in PostgreSQL
        assertTrue(customerOrderRepository.findById(savedOrder.getId()).isPresent());

        List<OrderItem> items = orderItemRepository.findByOrderId(savedOrder.getId());
        assertEquals(1, items.size());
        assertEquals(new BigDecimal("650.00"), items.get(0).getPrice());

        List<Payment> payments = paymentRepository.findByOrderId(savedOrder.getId());
        assertEquals(1, payments.size());
        assertEquals(new BigDecimal("300.00"), payments.get(0).getAmount());
        assertEquals("CASH", payments.get(0).getMode());

        // Clean up
        paymentRepository.deleteAll(payments);
        orderItemRepository.deleteAll(items);
        customerOrderRepository.delete(savedOrder);
    }

    @Test
    void testTransactionRollback_whenExceptionThrownMidway_databaseStaysEmpty() {
        assertNotNull(testUser, "Test requires at least one app_user in DB");

        Long testOrderNo = customerOrderRepository.getNextOrderNo();
        Customer customer = customerRepository.findAll().stream().findFirst().orElseThrow();

        // Calling failing transactional method that saves order row and then throws RuntimeException
        assertThrows(RuntimeException.class, () ->
                transactionTestHelper.saveOrderAndCrash(testOrderNo, customer, testUser, clock.instant())
        );

        // Verify: transaction rolled back completely! The order row does NOT exist in the database!
        assertFalse(
                customerOrderRepository.findByOrderNo(testOrderNo).isPresent(),
                "Order row should NOT exist in database because transaction rolled back!"
        );
    }

    // Helper component to test @Transactional boundary rollback
    @Component
    public static class TransactionTestHelper {
        @Autowired
        private CustomerOrderRepository customerOrderRepository;

        // Saves an order row and then deliberately throws a RuntimeException midway
        @Transactional
        public void saveOrderAndCrash(Long orderNo, Customer customer, AppUser user, Instant now) {
            CustomerOrder order = new CustomerOrder(orderNo, customer, user, now);
            customerOrderRepository.save(order);

            // Deliberate simulated crash midway through the transaction
            throw new RuntimeException("Simulated catastrophic crash midway through saving order!");
        }
    }
}
