package com.geetanjali.backend.service;

import com.geetanjali.backend.entity.Customer;
import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.entity.Payment;
import com.geetanjali.backend.enums.PaymentMode;
import com.geetanjali.backend.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// PaymentServiceTest: Unit tests for advance recording and total paid calculations.
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

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
        paymentService = new PaymentService(paymentRepository, fixedClock);

        Customer customer = new Customer("Ramesh Patel", "9876501234");
        order = new CustomerOrder(1001L, customer, null, fixedClock.instant());
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
    void testGetTotalPaid_sumsAllPaymentsForOrder() {
        Payment p1 = new Payment(order, new BigDecimal("200.00"), "CASH", LocalDate.now());
        Payment p2 = new Payment(order, new BigDecimal("350.50"), "UPI", LocalDate.now());
        when(paymentRepository.findByOrderId(10L)).thenReturn(List.of(p1, p2));

        BigDecimal total = paymentService.getTotalPaid(10L);

        assertEquals(new BigDecimal("550.50"), total);
    }
}
