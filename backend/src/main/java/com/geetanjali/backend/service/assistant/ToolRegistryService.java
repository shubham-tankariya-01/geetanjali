package com.geetanjali.backend.service.assistant;

import com.geetanjali.backend.dto.PendingPaymentRow;
import com.geetanjali.backend.dto.PendingPaymentsSummary;
import com.geetanjali.backend.dto.response.GarmentLatestMeasurement;
import com.geetanjali.backend.entity.AppUser;
import com.geetanjali.backend.entity.Customer;
import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.entity.MeasurementValue;
import com.geetanjali.backend.entity.OrderItem;
import com.geetanjali.backend.entity.Payment;
import com.geetanjali.backend.enums.ItemStatus;
import com.geetanjali.backend.enums.Role;
import com.geetanjali.backend.repository.AppUserRepository;
import com.geetanjali.backend.repository.CustomerOrderRepository;
import com.geetanjali.backend.repository.CustomerRepository;
import com.geetanjali.backend.repository.OrderItemRepository;
import com.geetanjali.backend.repository.PaymentRepository;
import com.geetanjali.backend.service.MeasurementService;
import com.geetanjali.backend.service.PaymentService;
import com.geetanjali.backend.service.assistant.model.ToolCall;
import com.geetanjali.backend.service.assistant.model.ToolDefinition;
import com.geetanjali.backend.service.assistant.model.ToolResult;
import com.geetanjali.backend.util.OrderNumberFormatter;
import com.geetanjali.backend.util.PhoneNumbers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ToolRegistryService: Registers and executes strongly-typed database query tools for the AI Assistant.
 * Guarantees strict multi-tenant isolation (scoped to authenticated user's shop) and Role-Based Access Control (RBAC).
 */
@Service
@Transactional(readOnly = true)
public class ToolRegistryService {

    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final MeasurementService measurementService;
    private final AppUserRepository appUserRepository;
    private final OrderNumberFormatter orderNumberFormatter;
    private final Clock clock;

    public ToolRegistryService(
            CustomerOrderRepository customerOrderRepository,
            OrderItemRepository orderItemRepository,
            CustomerRepository customerRepository,
            PaymentRepository paymentRepository,
            PaymentService paymentService,
            MeasurementService measurementService,
            AppUserRepository appUserRepository,
            OrderNumberFormatter orderNumberFormatter,
            Clock clock) {
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.customerRepository = customerRepository;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.measurementService = measurementService;
        this.appUserRepository = appUserRepository;
        this.orderNumberFormatter = orderNumberFormatter;
        this.clock = clock;
    }

    /**
     * Returns the list of tools available to the given user role.
     */
    public List<ToolDefinition> getAvailableTools(Role role) {
        List<ToolDefinition> tools = new ArrayList<>();

        // 1. getOrderStatus (Tailor + Owner)
        Map<String, Object> orderProps = new LinkedHashMap<>();
        orderProps.put("orderIdentifier", Map.of(
                "type", "string",
                "description", "The order number (e.g. '1001' or 'TS-2026-1001') or numeric database ID."
        ));
        tools.add(new ToolDefinition(
                "getOrderStatus",
                "Fetches delivery date, garment stitching statuses, total cost, and remaining payment balance for a specific order.",
                Map.of("type", "object", "properties", orderProps, "required", List.of("orderIdentifier"))
        ));

        // 2. getCustomerSummary (Tailor + Owner)
        Map<String, Object> custProps = new LinkedHashMap<>();
        custProps.put("query", Map.of(
                "type", "string",
                "description", "Customer name or 10-digit mobile phone number."
        ));
        tools.add(new ToolDefinition(
                "getCustomerSummary",
                "Finds customer contact details, total orders count (active vs completed), total money spent, outstanding balance, and garments with saved measurements.",
                Map.of("type", "object", "properties", custProps, "required", List.of("query"))
        ));

        // 3. getCustomerMeasurements (Tailor + Owner)
        Map<String, Object> measProps = new LinkedHashMap<>();
        measProps.put("customerQuery", Map.of(
                "type", "string",
                "description", "Customer name or 10-digit mobile phone number."
        ));
        measProps.put("garmentName", Map.of(
                "type", "string",
                "description", "Optional garment name filter (e.g. 'Pant', 'Shirt', 'Kurta', 'Blouse')."
        ));
        tools.add(new ToolDefinition(
                "getCustomerMeasurements",
                "Fetches saved measurement sizes and body dimension values (e.g., Chest, Waist, Length, Inseam, Collar) for a specific customer and garment.",
                Map.of("type", "object", "properties", measProps, "required", List.of("customerQuery"))
        ));

        // 4. getStaffOrders (Tailor + Owner)
        Map<String, Object> staffProps = new LinkedHashMap<>();
        staffProps.put("staffQuery", Map.of(
                "type", "string",
                "description", "Staff member, tailor, or dealer name or mobile phone number."
        ));
        tools.add(new ToolDefinition(
                "getStaffOrders",
                "Returns order metrics, active vs completed count, and recent orders booked or handled by a specific staff member, dealer, or tailor.",
                Map.of("type", "object", "properties", staffProps, "required", List.of("staffQuery"))
        ));

        // 5. getPendingWorkload (Tailor + Owner)
        Map<String, Object> workloadProps = new LinkedHashMap<>();
        workloadProps.put("urgency", Map.of(
                "type", "string",
                "enum", List.of("ALL", "TODAY", "OVERDUE", "UPCOMING"),
                "description", "Filter for stitching queue: 'ALL', 'TODAY' (due today), 'OVERDUE' (past delivery date), or 'UPCOMING'."
        ));
        tools.add(new ToolDefinition(
                "getPendingWorkload",
                "Returns the number and details of pending garments waiting to be stitched or ready for delivery.",
                Map.of("type", "object", "properties", workloadProps)
        ));

        // 6. searchOrders (Tailor + Owner)
        Map<String, Object> searchProps = new LinkedHashMap<>();
        searchProps.put("searchTerm", Map.of(
                "type", "string",
                "description", "Search query such as customer name, order number, garment note, dealer/staff name, or date keyword like 'today'."
        ));
        tools.add(new ToolDefinition(
                "searchOrders",
                "Searches studio orders by customer name, order number, garment notes, dealer/staff name, or date keywords.",
                Map.of("type", "object", "properties", searchProps, "required", List.of("searchTerm"))
        ));

        // Alias for Llama models that might guess 'searchMatrix' or 'queryOrders'
        tools.add(new ToolDefinition(
                "searchMatrix",
                "Alias for searchOrders. Searches studio orders and schedule records.",
                Map.of("type", "object", "properties", searchProps, "required", List.of("searchTerm"))
        ));

        // 7. getUnpaidOrders (OWNER ONLY)
        tools.add(new ToolDefinition(
                "getUnpaidOrders",
                "Owner-only tool. Returns the total outstanding unpaid balances across the studio and list of customers who owe money.",
                Map.of("type", "object", "properties", Collections.emptyMap())
        ));

        // 8. getRevenueMetrics (OWNER ONLY)
        Map<String, Object> revProps = new LinkedHashMap<>();
        revProps.put("period", Map.of(
                "type", "string",
                "enum", List.of("THIS_MONTH", "LAST_MONTH", "TODAY", "ALL_TIME"),
                "description", "Revenue time frame: 'THIS_MONTH', 'LAST_MONTH', 'TODAY', or 'ALL_TIME'."
        ));
        tools.add(new ToolDefinition(
                "getRevenueMetrics",
                "Owner-only tool. Aggregates total shop revenue, collection breakdown, and payment transaction counts for a given period.",
                Map.of("type", "object", "properties", revProps)
        ));

        return tools;
    }

    /**
     * Executes a tool call safely with tenant and role enforcement.
     */
    public ToolResult executeTool(Long shopId, Role role, ToolCall call) {
        String toolName = call.getName();
        Map<String, Object> args = call.getArguments() != null ? call.getArguments() : Collections.emptyMap();

        try {
            switch (toolName) {
                case "getOrderStatus":
                    return executeGetOrderStatus(shopId, args);

                case "getCustomerSummary":
                    return executeGetCustomerSummary(shopId, args);

                case "getCustomerMeasurements":
                case "getMeasurementSizes":
                    return executeGetCustomerMeasurements(shopId, args);

                case "getStaffOrders":
                case "getDealerOrders":
                    return executeGetStaffOrders(shopId, args);

                case "getPendingWorkload":
                    return executeGetPendingWorkload(shopId, args);

                case "searchOrders":
                case "searchMatrix":
                case "queryOrders":
                    return executeSearchOrders(shopId, args);

                case "getUnpaidOrders":
                    if (!Role.OWNER.equals(role)) {
                        return ToolResult.error("Access Restricted: Unpaid receivables and financial summaries are restricted to Shop Owners.");
                    }
                    return executeGetUnpaidOrders(shopId);

                case "getRevenueMetrics":
                    if (!Role.OWNER.equals(role)) {
                        return ToolResult.error("Access Restricted: Studio revenue and cash flow metrics are restricted to Shop Owners.");
                    }
                    return executeGetRevenueMetrics(shopId, args);

                default:
                    return ToolResult.error("Unknown tool: " + toolName);
            }
        } catch (Exception ex) {
            return ToolResult.error("Error executing tool " + toolName + ": " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Tool 1: getOrderStatus
    // -------------------------------------------------------------------------
    private ToolResult executeGetOrderStatus(Long shopId, Map<String, Object> args) {
        Object idVal = args.get("orderIdentifier");
        if (idVal == null) {
            return ToolResult.error("Order identifier was not provided.");
        }

        String raw = idVal.toString().trim();
        Optional<CustomerOrder> orderOpt = resolveOrder(shopId, raw);

        if (orderOpt.isEmpty()) {
            return ToolResult.ok(Map.of(
                    "found", false,
                    "message", "Order '" + raw + "' was not found in your studio."
            ));
        }

        CustomerOrder order = orderOpt.get();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("found", true);
        details.put("orderId", order.getId());
        details.put("orderNo", order.getOrderNo());
        details.put("formattedOrderNo", orderNumberFormatter.format(order));
        details.put("customerName", order.getCustomer().getName());
        details.put("customerMobile", order.getCustomer().getMobile());
        details.put("orderDate", order.getCreatedAt() != null ? order.getCreatedAt().toString() : "N/A");
        details.put("nearestDeliveryDate", order.getNearestDeliveryDate() != null ? order.getNearestDeliveryDate().toString() : "Not specified");
        details.put("totalAmount", order.getTotal().toPlainString());
        details.put("paidAmount", order.getPaid().toPlainString());
        details.put("balanceAmount", order.getBalance().toPlainString());
        details.put("isPaidInFull", order.isPaid());
        details.put("isAllDelivered", order.isAllDelivered());
        details.put("isCompleted", order.isCompleted());

        List<Map<String, Object>> items = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                Map<String, Object> itemMap = new LinkedHashMap<>();
                itemMap.put("itemId", item.getId());
                itemMap.put("garment", item.getGarment() != null ? item.getGarment().getName() : "Garment");
                itemMap.put("price", item.getPrice() != null ? item.getPrice().toPlainString() : "0");
                itemMap.put("status", item.getStatus() != null ? item.getStatus().name() : "PENDING");
                itemMap.put("deliveryDate", item.getDeliveryDate() != null ? item.getDeliveryDate().toString() : "N/A");
                itemMap.put("notes", item.getNotes() != null ? item.getNotes() : "");
                items.add(itemMap);
            }
        }
        details.put("items", items);

        return ToolResult.ok(details);
    }

    // -------------------------------------------------------------------------
    // Tool 2: getCustomerSummary
    // -------------------------------------------------------------------------
    private ToolResult executeGetCustomerSummary(Long shopId, Map<String, Object> args) {
        String query = args.getOrDefault("query", "").toString().trim();
        if (query.isEmpty()) {
            return ToolResult.error("Search query is required.");
        }

        List<Customer> matches;
        String normalizedMobile = PhoneNumbers.normalize(query);
        if (normalizedMobile.length() == 10) {
            matches = customerRepository.findByMobile(normalizedMobile).map(List::of).orElse(List.of());
        } else {
            matches = customerRepository.searchByNameOrMobile(query);
        }

        if (matches.isEmpty()) {
            return ToolResult.ok(Map.of(
                    "found", false,
                    "message", "No customer found matching '" + query + "'."
            ));
        }

        List<Map<String, Object>> customerSummaries = new ArrayList<>();
        for (Customer c : matches) {
            Map<String, Object> cMap = new LinkedHashMap<>();
            cMap.put("customerId", c.getId());
            cMap.put("name", c.getName());
            cMap.put("mobile", c.getMobile());

            List<CustomerOrder> orders = customerOrderRepository.findByCustomerIdOrderByCreatedAtDesc(c.getId());
            long activeOrdersCount = orders.stream().filter(o -> !o.isCompleted()).count();
            long completedOrdersCount = orders.stream().filter(CustomerOrder::isCompleted).count();
            BigDecimal totalSpent = orders.stream().map(CustomerOrder::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalBalance = orders.stream().map(CustomerOrder::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);

            cMap.put("totalOrdersCount", orders.size());
            cMap.put("activeOrdersCount", activeOrdersCount);
            cMap.put("completedOrdersCount", completedOrdersCount);
            cMap.put("totalSpent", "Rs. " + totalSpent.toPlainString());
            cMap.put("totalBalanceDue", "Rs. " + totalBalance.toPlainString());

            List<GarmentLatestMeasurement> measList = measurementService.getLatestPerGarment(c);
            List<String> measuredGarments = measList.stream()
                    .filter(GarmentLatestMeasurement::hasMeasurement)
                    .map(glm -> glm.garment() != null ? glm.garment().getName() : "Item")
                    .collect(Collectors.toList());
            cMap.put("garmentsWithMeasurements", measuredGarments);

            List<Map<String, Object>> recentOrders = orders.stream().limit(5).map(o -> {
                Map<String, Object> oMap = new LinkedHashMap<>();
                oMap.put("orderId", o.getId());
                oMap.put("orderNo", o.getOrderNo());
                oMap.put("formattedNo", orderNumberFormatter.format(o));
                oMap.put("nearestDeliveryDate", o.getNearestDeliveryDate() != null ? o.getNearestDeliveryDate().toString() : "N/A");
                oMap.put("total", o.getTotal().toPlainString());
                oMap.put("balance", o.getBalance().toPlainString());
                oMap.put("isCompleted", o.isCompleted());
                return oMap;
            }).collect(Collectors.toList());

            cMap.put("recentOrders", recentOrders);
            customerSummaries.add(cMap);
        }

        return ToolResult.ok(Map.of("found", true, "customers", customerSummaries));
    }

    // -------------------------------------------------------------------------
    // Tool: getCustomerMeasurements
    // -------------------------------------------------------------------------
    private ToolResult executeGetCustomerMeasurements(Long shopId, Map<String, Object> args) {
        String query = args.getOrDefault("customerQuery", "").toString().trim();
        if (query.isEmpty()) {
            query = args.getOrDefault("query", "").toString().trim();
        }
        if (query.isEmpty()) {
            return ToolResult.error("Customer query (name or mobile number) is required.");
        }

        String targetGarment = args.containsKey("garmentName") && args.get("garmentName") != null
                ? args.get("garmentName").toString().trim().toLowerCase()
                : "";

        List<Customer> matches;
        String normalizedMobile = PhoneNumbers.normalize(query);
        if (normalizedMobile.length() == 10) {
            matches = customerRepository.findByMobile(normalizedMobile).map(List::of).orElse(List.of());
        } else {
            matches = customerRepository.searchByNameOrMobile(query);
        }

        if (matches.isEmpty()) {
            return ToolResult.ok(Map.of(
                    "found", false,
                    "message", "No customer found matching '" + query + "'."
            ));
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (Customer c : matches) {
            Map<String, Object> cMap = new LinkedHashMap<>();
            cMap.put("customerId", c.getId());
            cMap.put("customerName", c.getName());
            cMap.put("customerMobile", c.getMobile());

            List<GarmentLatestMeasurement> latestList = measurementService.getLatestPerGarment(c);
            List<Map<String, Object>> recordedGarments = new ArrayList<>();

            for (GarmentLatestMeasurement glm : latestList) {
                if (glm.hasMeasurement() && glm.values() != null && !glm.values().isEmpty()) {
                    String gName = glm.garment() != null ? glm.garment().getName() : "Garment";
                    if (!targetGarment.isEmpty() && !gName.toLowerCase().contains(targetGarment)) {
                        continue;
                    }

                    Map<String, Object> gMap = new LinkedHashMap<>();
                    gMap.put("garmentName", gName);
                    gMap.put("recordedDate", glm.measurement().getCreatedAt() != null ? glm.measurement().getCreatedAt().toString() : "N/A");

                    Map<String, String> sizeValues = new LinkedHashMap<>();
                    for (MeasurementValue val : glm.values()) {
                        String fieldLabel = val.getField() != null ? val.getField().getLabel() : "Field";
                        String unit = val.getField() != null && val.getField().getUnit() != null ? val.getField().getUnit() : "inch";
                        sizeValues.put(fieldLabel, val.getValue().toPlainString() + " " + unit);
                    }
                    gMap.put("sizes", sizeValues);
                    recordedGarments.add(gMap);
                }
            }

            cMap.put("measurements", recordedGarments);
            cMap.put("hasMeasurements", !recordedGarments.isEmpty());
            if (recordedGarments.isEmpty()) {
                cMap.put("note", "No saved measurements found" + (!targetGarment.isEmpty() ? " for " + targetGarment : "") + ".");
            }
            results.add(cMap);
        }

        return ToolResult.ok(Map.of("found", true, "customers", results));
    }

    // -------------------------------------------------------------------------
    // Tool: getStaffOrders / getDealerOrders
    // -------------------------------------------------------------------------
    private ToolResult executeGetStaffOrders(Long shopId, Map<String, Object> args) {
        String rawQuery = args.getOrDefault("staffQuery", "").toString().trim();
        if (rawQuery.isEmpty()) {
            rawQuery = args.getOrDefault("query", "").toString().trim();
        }
        if (rawQuery.isEmpty()) {
            return ToolResult.error("Staff or dealer query (name or mobile number) is required.");
        }

        final String targetQuery = rawQuery.toLowerCase();
        List<AppUser> matchedUsers = appUserRepository.findByMobileOrName(rawQuery);
        if (matchedUsers.isEmpty()) {
            matchedUsers = appUserRepository.findAll().stream()
                    .filter(u -> u.getShop() != null && Objects.equals(u.getShop().getId(), shopId))
                    .filter(u -> u.getFullName() != null && u.getFullName().toLowerCase().contains(targetQuery))
                    .collect(Collectors.toList());
        } else {
            matchedUsers = matchedUsers.stream()
                    .filter(u -> u.getShop() != null && Objects.equals(u.getShop().getId(), shopId))
                    .collect(Collectors.toList());
        }

        if (matchedUsers.isEmpty()) {
            return ToolResult.ok(Map.of(
                    "found", false,
                    "message", "No staff member or dealer found matching '" + rawQuery + "' in this shop."
            ));
        }

        List<Map<String, Object>> staffSummaries = new ArrayList<>();
        for (AppUser user : matchedUsers) {
            List<CustomerOrder> orders = customerOrderRepository.findByCreatedByIdOrderByCreatedAtDesc(user.getId());
            long activeCount = orders.stream().filter(o -> !o.isCompleted()).count();
            long completedCount = orders.stream().filter(CustomerOrder::isCompleted).count();

            Map<String, Object> sMap = new LinkedHashMap<>();
            sMap.put("staffId", user.getId());
            sMap.put("staffName", user.getFullName());
            sMap.put("role", user.getRole() != null ? user.getRole().name() : "TAILOR");
            sMap.put("mobile", user.getMobile());
            sMap.put("totalOrdersBooked", orders.size());
            sMap.put("activeOrdersCount", activeCount);
            sMap.put("completedOrdersCount", completedCount);

            List<Map<String, Object>> recentOrders = orders.stream().limit(10).map(o -> {
                Map<String, Object> oMap = new LinkedHashMap<>();
                oMap.put("orderId", o.getId());
                oMap.put("formattedNo", orderNumberFormatter.format(o));
                oMap.put("customerName", o.getCustomer() != null ? o.getCustomer().getName() : "Unknown");
                oMap.put("total", o.getTotal().toPlainString());
                oMap.put("balance", o.getBalance().toPlainString());
                oMap.put("deliveryDate", o.getNearestDeliveryDate() != null ? o.getNearestDeliveryDate().toString() : "N/A");
                oMap.put("isCompleted", o.isCompleted());
                return oMap;
            }).collect(Collectors.toList());

            sMap.put("recentOrders", recentOrders);
            staffSummaries.add(sMap);
        }

        return ToolResult.ok(Map.of("found", true, "staffMembers", staffSummaries));
    }

    // -------------------------------------------------------------------------
    // Tool 3: getPendingWorkload
    // -------------------------------------------------------------------------
    private ToolResult executeGetPendingWorkload(Long shopId, Map<String, Object> args) {
        String urgency = args.getOrDefault("urgency", "ALL").toString().toUpperCase();
        LocalDate today = LocalDate.now(clock);

        List<CustomerOrder> allActive = customerOrderRepository.findAll().stream()
                .filter(o -> !o.isCompleted())
                .filter(o -> matchesShop(o, shopId))
                .collect(Collectors.toList());

        List<Map<String, Object>> workloadList = new ArrayList<>();
        int totalPendingItems = 0;
        int totalDoneItems = 0;

        for (CustomerOrder o : allActive) {
            if (o.getItems() == null) continue;

            LocalDate orderDelivery = o.getNearestDeliveryDate();
            boolean include = true;
            if ("TODAY".equals(urgency) && (orderDelivery == null || !orderDelivery.isEqual(today))) {
                include = false;
            } else if ("OVERDUE".equals(urgency) && (orderDelivery == null || !orderDelivery.isBefore(today))) {
                include = false;
            } else if ("UPCOMING".equals(urgency) && (orderDelivery == null || !orderDelivery.isAfter(today))) {
                include = false;
            }

            if (!include) continue;

            List<Map<String, Object>> items = new ArrayList<>();
            for (OrderItem it : o.getItems()) {
                if (!it.isDelivered()) {
                    if (it.getStatus() == ItemStatus.DONE) totalDoneItems++;
                    else totalPendingItems++;

                    items.add(Map.of(
                            "garment", it.getGarment() != null ? it.getGarment().getName() : "Item",
                            "status", it.getStatus() != null ? it.getStatus().name() : "PENDING",
                            "deliveryDate", it.getDeliveryDate() != null ? it.getDeliveryDate().toString() : "N/A"
                    ));
                }
            }

            if (!items.isEmpty()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("orderId", o.getId());
                row.put("formattedOrderNo", orderNumberFormatter.format(o));
                row.put("customerName", o.getCustomer().getName());
                row.put("deliveryDate", orderDelivery != null ? orderDelivery.toString() : "N/A");
                row.put("items", items);
                workloadList.add(row);
            }
        }

        // Sort by earliest delivery
        workloadList.sort(Comparator.comparing(m -> m.get("deliveryDate").toString()));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("filter", urgency);
        summary.put("activeOrdersCount", workloadList.size());
        summary.put("pendingStitchingGarments", totalPendingItems);
        summary.put("readyForTrialGarments", totalDoneItems);
        summary.put("orders", workloadList.stream().limit(15).collect(Collectors.toList()));

        return ToolResult.ok(summary);
    }

    // -------------------------------------------------------------------------
    // Tool 4: searchOrders
    // -------------------------------------------------------------------------
    private ToolResult executeSearchOrders(Long shopId, Map<String, Object> args) {
        String term = args.getOrDefault("searchTerm", "").toString().trim().toLowerCase();
        if (term.isEmpty()) {
            return ToolResult.error("Search term is required.");
        }

        List<CustomerOrder> all = customerOrderRepository.findAll().stream()
                .filter(o -> matchesShop(o, shopId))
                .collect(Collectors.toList());

        LocalDate today = LocalDate.now(clock);
        boolean isTodaySearch = "today".equals(term);

        List<Map<String, Object>> matches = new ArrayList<>();
        for (CustomerOrder o : all) {
            boolean match = false;
            String formatted = orderNumberFormatter.format(o).toLowerCase();
            String rawNo = String.valueOf(o.getOrderNo());
            String custName = o.getCustomer() != null ? o.getCustomer().getName().toLowerCase() : "";
            String custPhone = o.getCustomer() != null ? o.getCustomer().getMobile() : "";
            String staffName = o.getCreatedBy() != null ? o.getCreatedBy().getFullName().toLowerCase() : "";

            if (isTodaySearch && o.getNearestDeliveryDate() != null && o.getNearestDeliveryDate().isEqual(today)) {
                match = true;
            } else if (formatted.contains(term) || rawNo.contains(term) || custName.contains(term) || custPhone.contains(term) || staffName.contains(term)) {
                match = true;
            } else if (o.getItems() != null) {
                for (OrderItem it : o.getItems()) {
                    if (it.getGarment() != null && it.getGarment().getName().toLowerCase().contains(term)) {
                        match = true;
                        break;
                    }
                    if (it.getNotes() != null && it.getNotes().toLowerCase().contains(term)) {
                        match = true;
                        break;
                    }
                }
            }

            if (match) {
                matches.add(Map.of(
                        "orderId", o.getId(),
                        "formattedNo", orderNumberFormatter.format(o),
                        "customerName", o.getCustomer() != null ? o.getCustomer().getName() : "Unknown",
                        "deliveryDate", o.getNearestDeliveryDate() != null ? o.getNearestDeliveryDate().toString() : "N/A",
                        "balance", o.getBalance().toPlainString(),
                        "isCompleted", o.isCompleted()
                ));
            }
        }

        return ToolResult.ok(Map.of("searchTerm", term, "matchesFound", matches.size(), "orders", matches.stream().limit(10).collect(Collectors.toList())));
    }

    // -------------------------------------------------------------------------
    // Tool 5: getUnpaidOrders (OWNER ONLY)
    // -------------------------------------------------------------------------
    private ToolResult executeGetUnpaidOrders(Long shopId) {
        PendingPaymentsSummary summary = paymentService.getPendingPaymentsSummary();
        BigDecimal totalPending = summary != null ? summary.totalOutstanding() : BigDecimal.ZERO;

        List<Map<String, Object>> rows = new ArrayList<>();
        if (summary != null && summary.rows() != null) {
            for (PendingPaymentRow r : summary.rows()) {
                rows.add(Map.of(
                        "orderId", r.orderId(),
                        "formattedOrderNo", r.orderNumber(),
                        "customerName", r.customerName(),
                        "customerMobile", r.customerPhone(),
                        "balanceAmount", r.balance() != null ? r.balance().toPlainString() : "0",
                        "dueText", r.dueText() != null ? r.dueText() : ""
                ));
            }
        }

        return ToolResult.ok(Map.of(
                "totalOutstandingReceivables", "Rs. " + (totalPending != null ? totalPending.toPlainString() : "0"),
                "unpaidOrdersCount", rows.size(),
                "orders", rows.stream().limit(20).collect(Collectors.toList())
        ));
    }

    // -------------------------------------------------------------------------
    // Tool 6: getRevenueMetrics (OWNER ONLY)
    // -------------------------------------------------------------------------
    private ToolResult executeGetRevenueMetrics(Long shopId, Map<String, Object> args) {
        String period = args.getOrDefault("period", "THIS_MONTH").toString().toUpperCase();
        LocalDate today = LocalDate.now(clock);

        List<Payment> payments = paymentRepository.findAll();
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;

        for (Payment p : payments) {
            if (p.getOrder() != null && matchesShop(p.getOrder(), shopId)) {
                LocalDate paidOn = p.getPaidOn();
                if (paidOn == null) continue;

                boolean include = false;
                if ("TODAY".equals(period)) {
                    include = paidOn.isEqual(today);
                } else if ("THIS_MONTH".equals(period)) {
                    include = paidOn.getYear() == today.getYear() && paidOn.getMonth() == today.getMonth();
                } else if ("LAST_MONTH".equals(period)) {
                    YearMonth lastMonth = YearMonth.from(today).minusMonths(1);
                    include = paidOn.getYear() == lastMonth.getYear() && paidOn.getMonth() == lastMonth.getMonth();
                } else if ("ALL_TIME".equals(period)) {
                    include = true;
                }

                if (include && p.getAmount() != null) {
                    sum = sum.add(p.getAmount());
                    count++;
                }
            }
        }

        return ToolResult.ok(Map.of(
                "period", period,
                "totalCollectedRevenue", "Rs. " + sum.toPlainString(),
                "paymentTransactionsCount", count
        ));
    }

    // Helper: Scopes check to current shop
    private boolean matchesShop(CustomerOrder order, Long shopId) {
        if (shopId == null || order == null) return true;
        if (order.getCreatedBy() != null && order.getCreatedBy().getShop() != null) {
            return Objects.equals(order.getCreatedBy().getShop().getId(), shopId);
        }
        return true;
    }

    // Helper: Resolves order by raw input (ID or OrderNo or "TS-2026-1001")
    private Optional<CustomerOrder> resolveOrder(Long shopId, String raw) {
        // Try formatted sequence (strip letters and dashes if like TS-2026-1001)
        String cleaned = raw.replaceAll("[^0-9]", "");
        if (!cleaned.isEmpty()) {
            try {
                long num = Long.parseLong(cleaned);
                // First try orderNo (e.g. 1001)
                Optional<CustomerOrder> byNo = customerOrderRepository.findByOrderNo(num);
                if (byNo.isPresent() && matchesShop(byNo.get(), shopId)) {
                    return byNo;
                }
                // Try id
                Optional<CustomerOrder> byId = customerOrderRepository.findById(num);
                if (byId.isPresent() && matchesShop(byId.get(), shopId)) {
                    return byId;
                }
            } catch (NumberFormatException ignored) {}
        }
        return Optional.empty();
    }
}
