package com.geetanjali.backend.dto.calendar;

import java.time.LocalDate;
import java.util.List;

// MonthCalendarDto: Contains full month schedule, boxed grid days, and sidebar metrics.
public record MonthCalendarDto(
        int year,
        int month,
        String monthName,
        String yearMonthTitle,
        LocalDate prevMonthDate,
        LocalDate nextMonthDate,
        long totalOrdersInMonth,
        long totalItemsInMonth,
        int greenDaysCount,
        int yellowDaysCount,
        int redDaysCount,
        List<CalendarDaySummaryDto> gridDays,
        List<TopDeliveryDayDto> topDeliveryDays
) {}
