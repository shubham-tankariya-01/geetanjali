package com.geetanjali.backend.service;

import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.entity.Payment;
import com.geetanjali.backend.enums.PaymentMode;
import com.geetanjali.backend.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

// PaymentService: Business service managing payments and advance deposits for orders.
// In Express, this corresponds to `services/paymentService.js`.
//
// @Service = Tells Spring to instantiate this class as a managed business service bean.
@Service
// @Transactional(readOnly = true) = Defaults database interactions to read-only for performance.
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final Clock clock;

    // Constructor injection: Spring injects the PaymentRepository and Asia/Kolkata Clock bean.
    // Inputs: paymentRepository (PaymentRepository), clock (Clock).
    public PaymentService(PaymentRepository paymentRepository, Clock clock) {
        this.paymentRepository = paymentRepository;
        this.clock = clock;
    }

    // Records an advance deposit payment for an order if the amount is greater than zero.
    // If amount is zero or null, returns null without writing to the database.
    //
    // Inputs:
    //   order (CustomerOrder) - overarching order entity.
    //   amount (BigDecimal) - advance money received (e.g. ₹200.00).
    //   mode (PaymentMode) - CASH or UPI.
    // Output: Payment - saved payment entity, or null if no advance was paid.
    //
    // @Transactional = Runs in a read-write transaction.
    @Transactional
    public Payment recordAdvance(CustomerOrder order, BigDecimal amount, PaymentMode mode) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        // Today always comes from the injected Clock (Asia/Kolkata), never LocalDate.now()
        LocalDate today = clock.instant().atZone(clock.getZone()).toLocalDate();
        String paymentMode = (mode != null) ? mode.name() : PaymentMode.CASH.name();

        Payment payment = new Payment(order, amount, paymentMode, today);
        return paymentRepository.save(payment);
    }

    // Finds all payments recorded for an order.
    // Input: orderId (Long).
    // Output: List<Payment>.
    public List<Payment> findByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    // Calculates the total sum of payments received so far for an order.
    // Input: orderId (Long).
    // Output: BigDecimal - total money collected.
    public BigDecimal getTotalPaid(Long orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        BigDecimal total = BigDecimal.ZERO;
        for (Payment p : payments) {
            if (p.getAmount() != null) {
                total = total.add(p.getAmount());
            }
        }
        return total;
    }
}
