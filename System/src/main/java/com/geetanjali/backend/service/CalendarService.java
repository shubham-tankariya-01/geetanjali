package com.geetanjali.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geetanjali.backend.dto.calendar.CalendarDaySummaryDto;
import com.geetanjali.backend.dto.calendar.CalendarOrderItemDto;
import com.geetanjali.backend.dto.calendar.DayCapacityDto;
import com.geetanjali.backend.dto.calendar.MonthCalendarDto;
import com.geetanjali.backend.dto.calendar.TopDeliveryDayDto;
import com.geetanjali.backend.entity.OrderItem;
import com.geetanjali.backend.repository.OrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// CalendarService: Manages studio delivery scheduling, daily capacity rules,
// overbooking detection (5 soft limit, 10 hard limit), and monthly delivery metrics.
@Service
@Transactional(readOnly = true)
public class CalendarService {

    private final OrderItemRepository orderItemRepository;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter TOP_DAY_FORMATTER = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH);

    @Autowired
    public CalendarService(OrderItemRepository orderItemRepository, Clock clock, @Autowired(required = false) ObjectMapper objectMapper) {
        this.orderItemRepository = orderItemRepository;
        this.clock = clock;
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
    }

    // Evaluates capacity and overbooking limits for a single delivery date.
    // Standard limit: 5 orders.
    // Overbooking window: 5 to 9 orders (requires confirmation popup).
    // Maximum capacity limit: 10 orders (hard block).
    public DayCapacityDto getDayCapacity(LocalDate date) {
        long orderCount = orderItemRepository.countDistinctOrdersByDeliveryDate(date);
        long itemCount = orderItemRepository.countItemsByDeliveryDate(date);
        return DayCapacityDto.of(date, orderCount, itemCount);
    }

    // Builds the full studio boxed month view with 7-column Sun..Sat grid,
    // order tags, color-coded capacity indicators, and monthly top delivery days sidebar.
    public MonthCalendarDto getMonthCalendar(int year, int monthValue) {
        LocalDate today = LocalDate.now(clock);
        LocalDate firstDayOfMonth = LocalDate.of(year, monthValue, 1);
        LocalDate lastDayOfMonth = firstDayOfMonth.plusMonths(1).minusDays(1);

        // Standard 7-day Sunday-first layout: Sunday = 0, Monday = 1, ..., Saturday = 6
        int startDayOfWeek = firstDayOfMonth.getDayOfWeek().getValue() % 7;
        LocalDate gridStartDate = firstDayOfMonth.minusDays(startDayOfWeek);

        int endDayOfWeek = lastDayOfMonth.getDayOfWeek().getValue() % 7;
        int trailingDays = 6 - endDayOfWeek;
        LocalDate gridEndDate = lastDayOfMonth.plusDays(trailingDays);

        // Ensure minimum 35 cells (5 weeks) or 42 cells (6 weeks) for consistent visual grid
        long totalDays = ChronoUnit.DAYS.between(gridStartDate, gridEndDate) + 1;
        if (totalDays < 35) {
            gridEndDate = gridEndDate.plusDays(35 - totalDays);
        } else if (totalDays > 35 && totalDays < 42) {
            gridEndDate = gridEndDate.plusDays(42 - totalDays);
        }

        // Fetch all items within grid range in a single query
        List<OrderItem> items = orderItemRepository.findItemsBetweenDates(gridStartDate, gridEndDate);
        Map<LocalDate, List<OrderItem>> itemsByDate = items.stream()
                .collect(Collectors.groupingBy(OrderItem::getDeliveryDate));

        List<CalendarDaySummaryDto> gridDays = new ArrayList<>();
        Set<Long> uniqueOrdersInMonth = new HashSet<>();
        long totalItemsInMonth = 0;
        int greenDays = 0;
        int yellowDays = 0;
        int redDays = 0;

        List<TopDeliveryDayDto> candidates = new ArrayList<>();

        LocalDate currentDate = gridStartDate;
        while (!currentDate.isAfter(gridEndDate)) {
            boolean isCurrentMonth = (currentDate.getYear() == year && currentDate.getMonthValue() == monthValue);
            boolean isToday = currentDate.equals(today);

            List<OrderItem> dayItems = itemsByDate.getOrDefault(currentDate, Collections.emptyList());

            Set<Long> dayOrderIds = new HashSet<>();
            List<CalendarOrderItemDto> itemDtos = new ArrayList<>();

            for (OrderItem item : dayItems) {
                if (item.getOrder() != null) {
                    dayOrderIds.add(item.getOrder().getId());
                    if (isCurrentMonth) {
                        uniqueOrdersInMonth.add(item.getOrder().getId());
                    }
                }
                if (isCurrentMonth) {
                    totalItemsInMonth++;
                }

                itemDtos.add(new CalendarOrderItemDto(
                        item.getId(),
                        (item.getOrder() != null) ? item.getOrder().getId() : null,
                        (item.getOrder() != null) ? item.getOrder().getOrderNo() : null,
                        (item.getOrder() != null && item.getOrder().getCustomer() != null) ? item.getOrder().getCustomer().getName() : "Unknown",
                        (item.getOrder() != null && item.getOrder().getCustomer() != null) ? item.getOrder().getCustomer().getMobile() : "",
                        (item.getGarment() != null) ? item.getGarment().getName() : "Garment",
                        (item.getStatus() != null) ? item.getStatus().name() : "PENDING",
                        item.getPrice(),
                        (item.getOrder() != null) ? item.getOrder().getBalance() : null
                ));
            }

            long orderCount = dayOrderIds.size();
            long itemCount = dayItems.size();

            String color;
            String label;
            if (orderCount == 0) {
                color = "NONE";
                label = "";
            } else if (orderCount < 4) {
                color = "GREEN";
                label = orderCount + (orderCount == 1 ? " order" : " orders");
                if (isCurrentMonth) greenDays++;
            } else if (orderCount <= 5) {
                color = "YELLOW";
                label = orderCount + " orders";
                if (isCurrentMonth) yellowDays++;
            } else {
                color = "RED";
                label = orderCount + " orders";
                if (isCurrentMonth) redDays++;
            }

            if (isCurrentMonth && orderCount > 0) {
                candidates.add(new TopDeliveryDayDto(
                        0,
                        currentDate,
                        currentDate.format(TOP_DAY_FORMATTER),
                        orderCount,
                        itemCount,
                        color
                ));
            }

            String itemsJson = "[]";
            try {
                itemsJson = objectMapper.writeValueAsString(itemDtos);
            } catch (JsonProcessingException e) {
                // Fallback to empty json array
            }

            gridDays.add(new CalendarDaySummaryDto(
                    currentDate,
                    currentDate.getDayOfMonth(),
                    isCurrentMonth,
                    isToday,
                    orderCount,
                    itemCount,
                    color,
                    label,
                    itemDtos,
                    itemsJson
            ));

            currentDate = currentDate.plusDays(1);
        }

        // Calculate Top Delivery Days sorted by order count DESC, then item count DESC
        candidates.sort(Comparator.comparingLong(TopDeliveryDayDto::orderCount)
                .thenComparingLong(TopDeliveryDayDto::itemCount)
                .reversed());

        List<TopDeliveryDayDto> topDeliveryDays = new ArrayList<>();
        int rank = 1;
        for (TopDeliveryDayDto cand : candidates) {
            topDeliveryDays.add(new TopDeliveryDayDto(
                    rank++,
                    cand.date(),
                    cand.formattedDate(),
                    cand.orderCount(),
                    cand.itemCount(),
                    cand.tagColor()
            ));
            if (rank > 5) break; // Top 5 peak days
        }

        String monthName = Month.of(monthValue).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String yearMonthTitle = monthName + " " + year;
        LocalDate prevMonth = firstDayOfMonth.minusMonths(1);
        LocalDate nextMonth = firstDayOfMonth.plusMonths(1);

        return new MonthCalendarDto(
                year,
                monthValue,
                monthName,
                yearMonthTitle,
                prevMonth,
                nextMonth,
                uniqueOrdersInMonth.size(),
                totalItemsInMonth,
                greenDays,
                yellowDays,
                redDays,
                gridDays,
                topDeliveryDays
        );
    }

    // Retrieves all items scheduled for delivery on a specific date.
    public List<CalendarOrderItemDto> getDayOrders(LocalDate date) {
        List<OrderItem> items = orderItemRepository.findItemsByDeliveryDate(date);
        List<CalendarOrderItemDto> dtos = new ArrayList<>();
        for (OrderItem item : items) {
            dtos.add(new CalendarOrderItemDto(
                    item.getId(),
                    (item.getOrder() != null) ? item.getOrder().getId() : null,
                    (item.getOrder() != null) ? item.getOrder().getOrderNo() : null,
                    (item.getOrder() != null && item.getOrder().getCustomer() != null) ? item.getOrder().getCustomer().getName() : "Unknown",
                    (item.getOrder() != null && item.getOrder().getCustomer() != null) ? item.getOrder().getCustomer().getMobile() : "",
                    (item.getGarment() != null) ? item.getGarment().getName() : "Garment",
                    (item.getStatus() != null) ? item.getStatus().name() : "PENDING",
                    item.getPrice(),
                    (item.getOrder() != null) ? item.getOrder().getBalance() : null
            ));
        }
        return dtos;
    }
}
