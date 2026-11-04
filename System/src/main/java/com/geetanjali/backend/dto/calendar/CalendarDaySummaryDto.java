package com.geetanjali.backend.dto.calendar;

import java.time.LocalDate;
import java.util.List;

// CalendarDaySummaryDto: Represents a single day box within the Google Calendar style grid.
public record CalendarDaySummaryDto(
        LocalDate date,
        int dayOfMonth,
        boolean isCurrentMonth,
        boolean isToday,
        long orderCount,
        long itemCount,
        String tagColor,       // "NONE", "GREEN" (< 4), "YELLOW" (4-5), "RED" (5+)
        String tagLabel,       // e.g. "3 orders", "5 orders"
        List<CalendarOrderItemDto> items,
        String itemsJson
) {}
