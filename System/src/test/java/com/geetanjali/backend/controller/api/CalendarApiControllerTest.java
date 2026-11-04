package com.geetanjali.backend.controller.api;

import com.geetanjali.backend.dto.calendar.CalendarOrderItemDto;
import com.geetanjali.backend.dto.calendar.DayCapacityDto;
import com.geetanjali.backend.dto.calendar.MonthCalendarDto;
import com.geetanjali.backend.service.CalendarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarApiControllerTest {

    @Mock
    private CalendarService calendarService;

    private CalendarApiController controller;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-11-04T10:00:00Z"), ZoneId.of("Asia/Kolkata"));
        controller = new CalendarApiController(calendarService, clock);
    }

    @Test
    @DisplayName("GET /api/calendar/capacity returns capacity status DTO")
    void testCheckCapacity() {
        LocalDate date = LocalDate.of(2026, 11, 15);
        DayCapacityDto dto = DayCapacityDto.of(date, 3, 4);

        when(calendarService.getDayCapacity(eq(date))).thenReturn(dto);

        ResponseEntity<DayCapacityDto> response = controller.checkCapacity(date);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().orderCount()).isEqualTo(3);
        assertThat(response.getBody().tagColor()).isEqualTo("GREEN");
        assertThat(response.getBody().available()).isTrue();
        assertThat(response.getBody().overbooking()).isFalse();
        assertThat(response.getBody().maxReached()).isFalse();
    }

    @Test
    @DisplayName("GET /api/calendar/month returns month calendar DTO")
    void testGetMonth() {
        MonthCalendarDto dto = new MonthCalendarDto(
                2026, 11, "November", "November 2026",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 12, 1),
                15, 22, 5, 2, 1,
                Collections.emptyList(),
                Collections.emptyList()
        );

        when(calendarService.getMonthCalendar(eq(2026), eq(11))).thenReturn(dto);

        ResponseEntity<MonthCalendarDto> response = controller.getMonth(2026, 11);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().year()).isEqualTo(2026);
        assertThat(response.getBody().month()).isEqualTo(11);
        assertThat(response.getBody().totalOrdersInMonth()).isEqualTo(15);
    }

    @Test
    @DisplayName("GET /api/calendar/day returns list of items for specified date")
    void testGetDayItems() {
        LocalDate date = LocalDate.of(2026, 11, 15);
        when(calendarService.getDayOrders(eq(date))).thenReturn(Collections.emptyList());

        ResponseEntity<List<CalendarOrderItemDto>> response = controller.getDayItems(date);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
    }
}
