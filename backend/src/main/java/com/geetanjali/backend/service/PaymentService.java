package com.geetanjali.backend.service;

import com.geetanjali.backend.dto.form.PaymentForm;
import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.entity.Payment;
import com.geetanjali.backend.enums.PaymentMode;
import com.geetanjali.backend.exception.BusinessRuleException;
import com.geetanjali.backend.exception.ResourceNotFoundException;
import com.geetanjali.backend.repository.CustomerOrderRepository;
import com.geetanjali.backend.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

// PaymentService: Business service managing payments, advance deposits, and settlement ledgers.
// In Express, this corresponds to `services/paymentService.js`.
//
// @Service = Tells Spring to instantiate this class as a managed business service bean.
@Service
// @Transactional(readOnly = true) = Defaults database interactions to read-only for queries.
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final Clock clock;

    // Full constructor injection: Spring supplies PaymentRepository, CustomerOrderRepository, and Clock bean.
    // Inputs: paymentRepository (PaymentRepository), customerOrderRepository (CustomerOrderRepository), clock (Clock).
    // @Autowired = Tells Spring to use this constructor when creating the bean with multiple constructor choices.
    @org.springframework.beans.factory.annotation.Autowired
    public PaymentService(PaymentRepository paymentRepository,
                          CustomerOrderRepository customerOrderRepository,
                          Clock clock) {
        this.paymentRepository = paymentRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.clock = clock;
    }

    // Convenience constructor for unit tests that only mock PaymentRepository and Clock.
    // Inputs: paymentRepository (PaymentRepository), clock (Clock).
    public PaymentService(PaymentRepository paymentRepository, Clock clock) {
        this(paymentRepository, null, clock);
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

    // Adds a payment against an existing order after verifying all business validation rules.
    // Inputs:
    //   orderId (Long) - ID of the order.
    //   form (PaymentForm) - DTO containing amount, mode, paidOn, and returnTo.
    // Output: Payment - persisted payment record.
    // Throws: ResourceNotFoundException if order does not exist.
    // Throws: BusinessRuleException if amount <= 0, amount > balance, or date is in the future.
    //
    // @Transactional = Runs inside a read-write transaction with rollback on runtime exceptions.
    @Transactional
    public Payment addPayment(Long orderId, PaymentForm form) {
        if (form == null) {
            throw new BusinessRuleException("Payment form cannot be null.");
        }
        return addPayment(orderId, form.getAmount(), form.getMode(), form.getPaidOn());
    }

    // Adds a payment with individual primitive/value arguments.
    // Inputs:
    //   orderId (Long), amount (BigDecimal), mode (PaymentMode), paidOn (LocalDate).
    // Output: Payment - saved payment entity.
    //
    // @Transactional = Runs inside a read-write transaction.
    @Transactional
    public Payment addPayment(Long orderId, BigDecimal amount, PaymentMode mode, LocalDate paidOn) {
        if (customerOrderRepository == null) {
            throw new IllegalStateException("CustomerOrderRepository must be configured to add payments.");
        }

        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Rule 1: Amount must be strictly greater than zero
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Payment amount must be greater than zero.");
        }

        // Rule 2: Amount must NOT exceed the current remaining balance
        // Pure arithmetic calculation: balance is derived on the fly (total - paid).
        BigDecimal balance = order.getBalance();
        if (amount.compareTo(balance) > 0) {
            throw new BusinessRuleException("Payment amount of Rs. " + amount + " cannot exceed remaining balance of Rs. " + balance + ".");
        }

        // Rule 3: Payment date must not be in the future relative to Asia/Kolkata clock
        LocalDate today = clock.instant().atZone(clock.getZone()).toLocalDate();
        if (paidOn == null) {
            paidOn = today;
        } else if (paidOn.isAfter(today)) {
            throw new BusinessRuleException("Payment date cannot be in the future.");
        }

        String paymentMode = (mode != null) ? mode.name() : PaymentMode.CASH.name();
        Payment payment = new Payment(order, amount, paymentMode, paidOn);
        return paymentRepository.save(payment);
    }

    // Finds all payments recorded for an order, sorted oldest first (by paidOn ASC, id ASC).
    // Input: orderId (Long).
    // Output: List<Payment> - chronological payment receipts.
    public List<Payment> findByOrderId(Long orderId) {
        return paymentRepository.findByOrderIdOrderByPaidOnAscIdAsc(orderId);
    }

    // Calculates total money collected so far across all payments for an order.
    // Input: orderId (Long).
    // Output: BigDecimal - cumulative total collected.
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
