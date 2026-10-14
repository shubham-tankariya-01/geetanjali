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
}
