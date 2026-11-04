package com.geetanjali.backend.dto.calendar;

import java.time.LocalDate;
import java.util.List;

// CalendarDaySummaryDto: Represents a single day cell within the studio production grid.
public record CalendarDaySummaryDto(
        LocalDate date,
        int dayOfMonth,
        boolean isCurrentMonth,
        boolean isToday,
        long orderCount,
        long itemCount,
        String tagColor,       // "NONE", "GREEN", "YELLOW", "RED"
        String tagLabel,       // e.g. "3 orders", "5 orders"
        List<CalendarOrderItemDto> items,
        String itemsJson
) {}
