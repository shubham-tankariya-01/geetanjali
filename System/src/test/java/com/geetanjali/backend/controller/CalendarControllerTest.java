package com.geetanjali.backend.controller;

import com.geetanjali.backend.dto.calendar.MonthCalendarDto;
import com.geetanjali.backend.service.CalendarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarControllerTest {

    @Mock
    private CalendarService calendarService;

    private CalendarController controller;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-11-04T10:00:00Z"), ZoneId.of("Asia/Kolkata"));
        controller = new CalendarController(calendarService, clock);
    }

    @Test
    @DisplayName("GET /calendar renders calendar/index view with calendar model attributes")
    void testShowCalendar() {
        MonthCalendarDto dto = new MonthCalendarDto(
                2026, 11, "November", "November 2026",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 12, 1),
                10, 15, 4, 1, 0,
                Collections.emptyList(),
                Collections.emptyList()
        );

        when(calendarService.getMonthCalendar(anyInt(), anyInt())).thenReturn(dto);

        Model model = new ConcurrentModel();
        String view = controller.showCalendar(null, null, model);

        assertThat(view).isEqualTo("calendar/index");
        assertThat(model.getAttribute("calendar")).isEqualTo(dto);
        assertThat(model.getAttribute("today")).isEqualTo(LocalDate.of(2026, 11, 4));
        assertThat(model.getAttribute("currentUri")).isEqualTo("/calendar");
    }
}
