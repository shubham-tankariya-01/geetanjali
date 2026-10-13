package com.geetanjali.backend.service;

import com.geetanjali.backend.dto.draft.OrderDraft;
import com.geetanjali.backend.dto.draft.OrderDraftItem;
import com.geetanjali.backend.dto.form.AdvanceForm;
import com.geetanjali.backend.entity.AppUser;
import com.geetanjali.backend.entity.Customer;
import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.entity.ItemPhoto;
import com.geetanjali.backend.entity.Measurement;
import com.geetanjali.backend.entity.OrderItem;
import com.geetanjali.backend.exception.ResourceNotFoundException;
import com.geetanjali.backend.repository.CustomerOrderRepository;
import com.geetanjali.backend.repository.CustomerRepository;
import com.geetanjali.backend.repository.ItemPhotoRepository;
import com.geetanjali.backend.repository.OrderItemRepository;
import com.geetanjali.backend.util.PhoneNumbers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

// OrderService: Core domain service managing customer orders, items, and atomic order placement.
// In Express, this corresponds to `services/orderService.js`.
//
// @Service = Tells Spring to instantiate this class as a managed business service bean.
@Service
// @Transactional(readOnly = true) = Defaults to read-only database operations for queries.
@Transactional(readOnly = true)
public class OrderService {

    private final CustomerRepository customerRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemPhotoRepository itemPhotoRepository;
    private final MeasurementService measurementService;
    private final PaymentService paymentService;
    private final Clock clock;

    // Constructor injection: Spring supplies all repositories and services automatically.
    public OrderService(
            CustomerRepository customerRepository,
            CustomerOrderRepository customerOrderRepository,
            OrderItemRepository orderItemRepository,
            ItemPhotoRepository itemPhotoRepository,
            MeasurementService measurementService,
            PaymentService paymentService,
            Clock clock) {
        this.customerRepository = customerRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.itemPhotoRepository = itemPhotoRepository;
        this.measurementService = measurementService;
        this.paymentService = paymentService;
        this.clock = clock;
    }

    // Places an entire customer order in a single, atomic database transaction.
    // 1. Normalizes mobile number and finds or creates the customer record in PostgreSQL.
    // 2. Fetches the next unique order number from sequence `order_no_seq`.
    // 3. Persists the CustomerOrder row with createdBy staff member and IST timestamp.
    // 4. For each draft garment item:
    //    a. Saves or deduplicates the measurement snapshot via MeasurementService.
    //    b. Creates and saves the OrderItem row with notes, price, and delivery date.
    //    c. Attaches any reference ItemPhoto records.
    // 5. If advance deposit > ₹0, logs one Payment row dated today.
    //
    // WHY @Transactional:
    // If any step throws a RuntimeException (e.g. database disconnect, constraint violation),
    // Spring automatically rolls back the ENTIRE transaction.
    // No orphaned customer, order, item, or photo rows will ever remain in PostgreSQL!
    //
    // Inputs:
    //   draft (OrderDraft) - the completed session cart with customer info and garment items.
    //   advanceForm (AdvanceForm) - advance amount and Cash/UPI payment mode.
    //   createdBy (AppUser) - the logged-in tailor/owner who booked this order.
    // Output: CustomerOrder - the newly persisted order entity.
    //
    // @Transactional = Runs in a read-write atomic transaction.
    @Transactional
    public CustomerOrder placeOrder(OrderDraft draft, AdvanceForm advanceForm, AppUser createdBy) {
        // Step 0: Guard against empty carts
        if (draft == null || draft.getItemCount() == 0) {
            throw new IllegalArgumentException("Cannot place order with zero garment items");
        }

        // Step 1: Normalize mobile and resolve customer (find existing or create new)
        String normalizedMobile = PhoneNumbers.normalize(draft.getCustomerMobile());
        Customer customer = resolveCustomer(draft, normalizedMobile);

        // Step 2: Retrieve next order number from sequence order_no_seq (e.g. 1001)
        Long orderNo = customerOrderRepository.getNextOrderNo();

        // Step 3: Create top-level CustomerOrder
        Instant now = clock.instant();
        CustomerOrder order = new CustomerOrder(orderNo, customer, createdBy, now);
        CustomerOrder savedOrder = customerOrderRepository.save(order);

        // Step 4: Persist each garment item, its measurement snapshot, and photos
        for (OrderDraftItem draftItem : draft.getItems()) {
            // Step 4a: Snapshot measurements (deduplicating if identical to past orders)
            Measurement measurement = null;
            if (draftItem.getMeasurementValues() != null && !draftItem.getMeasurementValues().isEmpty()) {
                measurement = measurementService.saveSnapshot(
                        customer,
                        draftItem.getGarment(),
                        draftItem.getMeasurementValues()
                );
            }

            // Step 4b: Save OrderItem entity
            OrderItem orderItem = new OrderItem(
                    savedOrder,
                    draftItem.getGarment(),
                    measurement,
                    draftItem.getNotes(),
                    draftItem.getPrice(),
                    draftItem.getDeliveryDate()
            );
            OrderItem savedItem = orderItemRepository.save(orderItem);

            // Step 4c: Attach ItemPhoto records if photos were captured
            if (draftItem.getPhotoUrls() != null && !draftItem.getPhotoUrls().isEmpty()) {
                List<String> photoUrls = draftItem.getPhotoUrls();
                List<String> photoPublicIds = draftItem.getPhotoPublicIds();

                for (int i = 0; i < photoUrls.size(); i++) {
                    String url = photoUrls.get(i);
                    String publicId = (photoPublicIds != null && i < photoPublicIds.size())
                            ? photoPublicIds.get(i)
                            : null;

                    ItemPhoto itemPhoto = new ItemPhoto(savedItem, url, publicId);
                    itemPhotoRepository.save(itemPhoto);
                }
            }
        }

        // Step 5: Record advance payment if customer made a deposit upfront
        if (advanceForm != null && advanceForm.getAdvanceAmount() != null) {
            BigDecimal advanceAmount = advanceForm.getAdvanceAmount();
            if (advanceAmount.compareTo(BigDecimal.ZERO) > 0) {
                paymentService.recordAdvance(savedOrder, advanceAmount, advanceForm.getMode());
            }
        }

        return savedOrder;
    }

    // Helper: Finds existing customer by ID or mobile, or creates a brand new Customer record.
    // Inputs: draft (OrderDraft), normalizedMobile (String).
    // Output: Customer entity.
    private Customer resolveCustomer(OrderDraft draft, String normalizedMobile) {
        if (draft.getCustomerId() != null) {
            return customerRepository.findById(draft.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + draft.getCustomerId()));
        }

        // Lookup by normalized mobile number
        Optional<Customer> existingOpt = customerRepository.findByMobile(normalizedMobile);
        if (existingOpt.isPresent()) {
            return existingOpt.get();
        }

        // Create new customer
        String name = (draft.getCustomerName() != null && !draft.getCustomerName().isBlank())
                ? draft.getCustomerName().trim()
                : "Walk-in Customer";

        Customer newCustomer = new Customer(name, normalizedMobile);
        return customerRepository.save(newCustomer);
    }

    // Finds an order by its primary key ID.
    // Input: id (Long).
    // Output: Optional<CustomerOrder>.
    public Optional<CustomerOrder> findById(Long id) {
        return customerOrderRepository.findById(id);
    }

    // Finds an order by its human-friendly order number (e.g. 1001).
    // Input: orderNo (Long).
    // Output: Optional<CustomerOrder>.
    public Optional<CustomerOrder> findByOrderNo(Long orderNo) {
        return customerOrderRepository.findByOrderNo(orderNo);
    }

    // Retrieves all items belonging to an order.
    // Input: orderId (Long).
    // Output: List<OrderItem>.
    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    // Retrieves up to 20 most recent orders across the shop for the home dashboard.
    // Input: None.
    // Output: List<CustomerOrder> - newest orders list.
    public List<CustomerOrder> getRecentOrders() {
        return customerOrderRepository.findTop20ByOrderByCreatedAtDesc();
    }

    // Retrieves an order by its primary key ID, or throws ResourceNotFoundException if missing.
    // Automatically mapped to HTTP 404 by GlobalExceptionHandler.
    // Input: id (Long).
    // Output: CustomerOrder entity.
    public CustomerOrder getOrderOrThrow(Long id) {
        return customerOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }
}
