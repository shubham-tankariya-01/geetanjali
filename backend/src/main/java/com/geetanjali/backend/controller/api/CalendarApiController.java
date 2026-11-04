package com.geetanjali.backend.controller.api;

import com.geetanjali.backend.dto.calendar.CalendarOrderItemDto;
import com.geetanjali.backend.dto.calendar.DayCapacityDto;
import com.geetanjali.backend.dto.calendar.MonthCalendarDto;
import com.geetanjali.backend.service.CalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

// CalendarApiController: Provides REST API endpoints for real-time delivery capacity checking,
// month schedule data, and day order breakdowns.
@RestController
@RequestMapping("/api/calendar")
public class CalendarApiController {

    private final CalendarService calendarService;
    private final Clock clock;

    @Autowired
    public CalendarApiController(CalendarService calendarService, Clock clock) {
        this.calendarService = calendarService;
        this.clock = clock;
    }

    // GET /api/calendar/capacity?date=2026-11-12
    // Checks the daily order count, available capacity, and overbooking warning flags.
    @GetMapping("/capacity")
    public ResponseEntity<DayCapacityDto> checkCapacity(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DayCapacityDto capacity = calendarService.getDayCapacity(date);
        return ResponseEntity.ok(capacity);
    }

    // GET /api/calendar/month?year=2026&month=11
    // Retrieves full monthly calendar grid data with top delivery days.
    @GetMapping("/month")
    public ResponseEntity<MonthCalendarDto> getMonth(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month) {
        LocalDate today = LocalDate.now(clock);
        int targetYear = (year != null && year >= 2000 && year <= 2100) ? year : today.getYear();
        int targetMonth = (month != null && month >= 1 && month <= 12) ? month : today.getMonthValue();

        MonthCalendarDto calendarDto = calendarService.getMonthCalendar(targetYear, targetMonth);
        return ResponseEntity.ok(calendarDto);
    }

    // GET /api/calendar/day?date=2026-11-12
    // Retrieves individual order items scheduled for delivery on the specified date.
    @GetMapping("/day")
    public ResponseEntity<List<CalendarOrderItemDto>> getDayItems(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<CalendarOrderItemDto> items = calendarService.getDayOrders(date);
        return ResponseEntity.ok(items);
    }
}
