package com.geetanjali.backend.service;

import com.geetanjali.backend.dto.form.PaymentForm;
import com.geetanjali.backend.entity.Customer;
import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.entity.OrderItem;
import com.geetanjali.backend.entity.Payment;
import com.geetanjali.backend.enums.PaymentMode;
import com.geetanjali.backend.exception.BusinessRuleException;
import com.geetanjali.backend.exception.ResourceNotFoundException;
import com.geetanjali.backend.repository.CustomerOrderRepository;
import com.geetanjali.backend.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// PaymentServiceTest: Unit tests for advance recording, addPayment business rules, and ledger calculations.
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    private Clock fixedClock;
    private PaymentService paymentService;
    private CustomerOrder order;

    @BeforeEach
    void setUp() {
        // Fixed IST clock: 2026-10-15
        fixedClock = Clock.fixed(
                Instant.parse("2026-10-15T10:00:00Z"),
                ZoneId.of("Asia/Kolkata")
        );
        paymentService = new PaymentService(paymentRepository, customerOrderRepository, fixedClock);

        Customer customer = new Customer("Ramesh Patel", "9876501234");
        order = new CustomerOrder(1001L, customer, null, fixedClock.instant());
        order.setId(10L);

        // Add an item with price 1000 so order total is 1000
        OrderItem item = new OrderItem(order, null, null, "Test", new BigDecimal("1000.00"), LocalDate.of(2026, 10, 20));
        order.setItems(List.of(item));
        // Add an advance of 200 so remaining balance is 800
        Payment advance = new Payment(order, new BigDecimal("200.00"), "UPI", LocalDate.of(2026, 10, 15));
        order.setPayments(List.of(advance));
    }

    @Test
    void testRecordAdvance_positiveAmount_savesPaymentWithTodayDate() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment payment = paymentService.recordAdvance(order, new BigDecimal("250.00"), PaymentMode.UPI);

        assertNotNull(payment);
        assertEquals(new BigDecimal("250.00"), payment.getAmount());
        assertEquals("UPI", payment.getMode());
        assertEquals(LocalDate.of(2026, 10, 15), payment.getPaidOn());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertEquals(order, captor.getValue().getOrder());
    }

    @Test
    void testRecordAdvance_zeroAmount_returnsNullAndDoesNotSave() {
        Payment payment = paymentService.recordAdvance(order, BigDecimal.ZERO, PaymentMode.CASH);

        assertNull(payment);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void testRecordAdvance_nullAmount_returnsNullAndDoesNotSave() {
        Payment payment = paymentService.recordAdvance(order, null, PaymentMode.CASH);

        assertNull(payment);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("addPayment successfully saves valid payment when amount <= balance and date <= today")
    void testAddPayment_validPayment_savesSuccessfully() {
        when(customerOrderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentForm form = new PaymentForm(new BigDecimal("400.00"), PaymentMode.CASH, LocalDate.of(2026, 10, 15), "/orders/10");
        Payment saved = paymentService.addPayment(10L, form);

        assertNotNull(saved);
        assertEquals(new BigDecimal("400.00"), saved.getAmount());
        assertEquals("CASH", saved.getMode());
        assertEquals(LocalDate.of(2026, 10, 15), saved.getPaidOn());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("addPayment throws BusinessRuleException when amount <= 0")
    void testAddPayment_amountZeroOrNegative_throwsException() {
        when(customerOrderRepository.findById(10L)).thenReturn(Optional.of(order));

        PaymentForm formZero = new PaymentForm(BigDecimal.ZERO, PaymentMode.CASH, LocalDate.of(2026, 10, 15), "/orders/10");
        assertThrows(BusinessRuleException.class, () -> paymentService.addPayment(10L, formZero));

        PaymentForm formNeg = new PaymentForm(new BigDecimal("-50.00"), PaymentMode.CASH, LocalDate.of(2026, 10, 15), "/orders/10");
        assertThrows(BusinessRuleException.class, () -> paymentService.addPayment(10L, formNeg));
    }

    @Test
    @DisplayName("addPayment throws BusinessRuleException when amount exceeds current balance")
    void testAddPayment_amountExceedsBalance_throwsException() {
        when(customerOrderRepository.findById(10L)).thenReturn(Optional.of(order));

        // Balance is 800 (1000 total - 200 advance). Attempting to pay 850 should fail.
        PaymentForm formExcess = new PaymentForm(new BigDecimal("850.00"), PaymentMode.UPI, LocalDate.of(2026, 10, 15), "/orders/10");
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> paymentService.addPayment(10L, formExcess));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("cannot exceed remaining balance"));
    }

    @Test
    @DisplayName("addPayment throws BusinessRuleException when payment date is in the future")
    void testAddPayment_futureDate_throwsException() {
        when(customerOrderRepository.findById(10L)).thenReturn(Optional.of(order));

        // Today is 2026-10-15. Date 2026-10-16 is in the future.
        PaymentForm formFuture = new PaymentForm(new BigDecimal("300.00"), PaymentMode.UPI, LocalDate.of(2026, 10, 16), "/orders/10");
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> paymentService.addPayment(10L, formFuture));
        assertEquals("Payment date cannot be in the future.", ex.getMessage());
    }

    @Test
    @DisplayName("addPayment throws ResourceNotFoundException when order does not exist")
    void testAddPayment_orderNotFound_throwsException() {
        when(customerOrderRepository.findById(99L)).thenReturn(Optional.empty());

        PaymentForm form = new PaymentForm(new BigDecimal("100.00"), PaymentMode.CASH, LocalDate.of(2026, 10, 15), "/orders/99");
        assertThrows(ResourceNotFoundException.class, () -> paymentService.addPayment(99L, form));
    }

    @Test
    void testGetTotalPaid_sumsAllPaymentsForOrder() {
        Payment p1 = new Payment(order, new BigDecimal("200.00"), "CASH", LocalDate.now());
        Payment p2 = new Payment(order, new BigDecimal("350.50"), "UPI", LocalDate.now());
        when(paymentRepository.findByOrderId(10L)).thenReturn(List.of(p1, p2));

        BigDecimal total = paymentService.getTotalPaid(10L);

        assertEquals(new BigDecimal("550.50"), total);
    }

    @Test
    @DisplayName("getPendingPaymentsSummary includes only orders with balance > 0 and sorts by earliest delivery date oldest first")
    void testGetPendingPaymentsSummary_filtersPaidOrdersAndSortsChronologically() {
        // Order 1: Earliest delivery Oct 20, balance 800 (from setUp)
        // Order 2: Fully paid order (Total 500, Paid 500)
        Customer customer2 = new Customer("Sita Devi", "9876509999");
        CustomerOrder order2 = new CustomerOrder(1002L, customer2, null, fixedClock.instant());
        order2.setId(20L);
        OrderItem item2 = new OrderItem(order2, null, null, "Saree", new BigDecimal("500.00"), LocalDate.of(2026, 10, 18));
        order2.setItems(List.of(item2));
        Payment payment2 = new Payment(order2, new BigDecimal("500.00"), "CASH", LocalDate.of(2026, 10, 15));
        order2.setPayments(List.of(payment2));

        // Order 3: Earliest delivery Oct 12 (earlier than Order 1), balance 300
        Customer customer3 = new Customer("Gita Sharma", "9876508888");
        CustomerOrder order3 = new CustomerOrder(1003L, customer3, null, fixedClock.instant());
        order3.setId(30L);
        OrderItem item3 = new OrderItem(order3, null, null, "Kurta", new BigDecimal("600.00"), LocalDate.of(2026, 10, 12));
        order3.setItems(List.of(item3));
        Payment payment3 = new Payment(order3, new BigDecimal("300.00"), "UPI", LocalDate.of(2026, 10, 14));
        order3.setPayments(List.of(payment3));

        when(customerOrderRepository.findAll()).thenReturn(List.of(order, order2, order3));

        com.geetanjali.backend.dto.PendingPaymentsSummary summary = paymentService.getPendingPaymentsSummary();

        // Total outstanding should be Order 1 (800) + Order 3 (300) = 1100 (Order 2 is excluded because balance is 0)
        assertEquals(new BigDecimal("1100.00"), summary.totalOutstanding());
        assertEquals(2, summary.rows().size());

        // Chronological order: Order 3 (Oct 12) must come before Order 1 (Oct 20)
        assertEquals(30L, summary.rows().get(0).orderId());
        assertEquals(new BigDecimal("300.00"), summary.rows().get(0).balance());
        assertEquals(10L, summary.rows().get(1).orderId());
        assertEquals(new BigDecimal("800.00"), summary.rows().get(1).balance());
    }

    @Test
    @DisplayName("computeDueText returns 'delivered N days ago' when any item in the order was delivered")
    void testComputeDueText_deliveredItem_returnsDeliveredNDaysAgo() {
        // Today is fixed at 2026-10-15
        OrderItem item = order.getItems().get(0);
        item.markDone(Instant.parse("2026-10-14T10:00:00Z"));
        // Delivered 2 days ago on 2026-10-13
        item.markDelivered(Instant.parse("2026-10-13T10:00:00Z"));

        String text = paymentService.computeDueText(order, LocalDate.of(2026, 10, 15));
        assertEquals("delivered 2 days ago", text);
    }

    @Test
    @DisplayName("computeDueText returns 'due in N days' when promised delivery date is in future")
    void testComputeDueText_futureDelivery_returnsDueInNDays() {
        // Today is 2026-10-15, item promised delivery is 2026-10-20 (5 days in future)
        String text = paymentService.computeDueText(order, LocalDate.of(2026, 10, 15));
        assertEquals("due in 5 days", text);
    }

    @Test
    @DisplayName("computeDueText returns 'overdue by N days' when delivery date is in past without delivery")
    void testComputeDueText_pastDelivery_returnsOverdueByNDays() {
        // Change item promised delivery to 3 days ago: 2026-10-12
        OrderItem item = order.getItems().get(0);
        item.setDeliveryDate(LocalDate.of(2026, 10, 12));

        String text = paymentService.computeDueText(order, LocalDate.of(2026, 10, 15));
        assertEquals("overdue by 3 days", text);
    }
}
