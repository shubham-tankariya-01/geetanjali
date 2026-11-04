package com.geetanjali.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geetanjali.backend.dto.calendar.DayCapacityDto;
import com.geetanjali.backend.dto.calendar.MonthCalendarDto;
import com.geetanjali.backend.entity.Customer;
import com.geetanjali.backend.entity.CustomerOrder;
import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.entity.OrderItem;
import com.geetanjali.backend.repository.OrderItemRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    private CalendarService calendarService;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-11-04T10:00:00Z"), ZoneId.of("Asia/Kolkata"));
        calendarService = new CalendarService(orderItemRepository, fixedClock, new ObjectMapper());
    }

    @Test
    @DisplayName("getDayCapacity: 2 orders -> Light load GREEN (< 4), available, no overbooking")
    void testGetDayCapacity_lightLoad() {
        LocalDate date = LocalDate.of(2026, 11, 15);
        when(orderItemRepository.countDistinctOrdersByDeliveryDate(date)).thenReturn(2L);
        when(orderItemRepository.countItemsByDeliveryDate(date)).thenReturn(3L);

        DayCapacityDto capacity = calendarService.getDayCapacity(date);

        assertThat(capacity.orderCount()).isEqualTo(2);
        assertThat(capacity.itemCount()).isEqualTo(3);
        assertThat(capacity.tagColor()).isEqualTo("GREEN");
        assertThat(capacity.available()).isTrue();
        assertThat(capacity.overbooking()).isFalse();
        assertThat(capacity.maxReached()).isFalse();
    }

    @Test
    @DisplayName("getDayCapacity: 5 orders -> Standard limit reached, overbooking window starts (YELLOW)")
    void testGetDayCapacity_standardLimit() {
        LocalDate date = LocalDate.of(2026, 11, 16);
        when(orderItemRepository.countDistinctOrdersByDeliveryDate(date)).thenReturn(5L);
        when(orderItemRepository.countItemsByDeliveryDate(date)).thenReturn(6L);

        DayCapacityDto capacity = calendarService.getDayCapacity(date);

        assertThat(capacity.orderCount()).isEqualTo(5);
        assertThat(capacity.tagColor()).isEqualTo("YELLOW");
        assertThat(capacity.available()).isFalse();
        assertThat(capacity.overbooking()).isTrue();
        assertThat(capacity.maxReached()).isFalse();
    }

    @Test
    @DisplayName("getDayCapacity: 7 orders -> Overbooking window (RED, 5+ orders, prompt confirmation)")
    void testGetDayCapacity_overbookingHighLoad() {
        LocalDate date = LocalDate.of(2026, 11, 17);
        when(orderItemRepository.countDistinctOrdersByDeliveryDate(date)).thenReturn(7L);
        when(orderItemRepository.countItemsByDeliveryDate(date)).thenReturn(8L);

        DayCapacityDto capacity = calendarService.getDayCapacity(date);

        assertThat(capacity.orderCount()).isEqualTo(7);
        assertThat(capacity.tagColor()).isEqualTo("RED");
        assertThat(capacity.available()).isFalse();
        assertThat(capacity.overbooking()).isTrue();
        assertThat(capacity.maxReached()).isFalse();
    }

    @Test
    @DisplayName("getDayCapacity: 10 orders -> Maximum capacity reached (RED, hard block)")
    void testGetDayCapacity_maxCapacityReached() {
        LocalDate date = LocalDate.of(2026, 11, 18);
        when(orderItemRepository.countDistinctOrdersByDeliveryDate(date)).thenReturn(10L);
        when(orderItemRepository.countItemsByDeliveryDate(date)).thenReturn(12L);

        DayCapacityDto capacity = calendarService.getDayCapacity(date);

        assertThat(capacity.orderCount()).isEqualTo(10);
        assertThat(capacity.tagColor()).isEqualTo("RED");
        assertThat(capacity.available()).isFalse();
        assertThat(capacity.overbooking()).isFalse();
        assertThat(capacity.maxReached()).isTrue();
    }

    @Test
    @DisplayName("getMonthCalendar: generates complete boxed grid and ranks top delivery days")
    void testGetMonthCalendar_generatesGridAndTopDays() {
        LocalDate delivery1 = LocalDate.of(2026, 11, 10);
        LocalDate delivery2 = LocalDate.of(2026, 11, 20);

        Customer customer = new Customer("Ketan Tankariya", "9876543210");
        CustomerOrder order1 = new CustomerOrder(1001L, customer, null, Instant.now());
        Garment shirt = new Garment("Shirt", 1, true);

        OrderItem item1 = new OrderItem(order1, shirt, null, "Fit notes", BigDecimal.valueOf(450), delivery1);
        OrderItem item2 = new OrderItem(order1, shirt, null, "Fit notes 2", BigDecimal.valueOf(500), delivery2);

        when(orderItemRepository.findItemsBetweenDates(any(), any())).thenReturn(List.of(item1, item2));

        MonthCalendarDto calendar = calendarService.getMonthCalendar(2026, 11);

        assertThat(calendar.year()).isEqualTo(2026);
        assertThat(calendar.month()).isEqualTo(11);
        assertThat(calendar.monthName()).isEqualTo("November");
        assertThat(calendar.yearMonthTitle()).isEqualTo("November 2026");
        assertThat(calendar.gridDays().size()).isGreaterThanOrEqualTo(35);
        assertThat(calendar.totalOrdersInMonth()).isEqualTo(1);
        assertThat(calendar.totalItemsInMonth()).isEqualTo(2);
        assertThat(calendar.topDeliveryDays()).isNotEmpty();
    }
}
