package com.geetanjali.backend.dto.calendar;

import java.time.LocalDate;

// TopDeliveryDayDto: Captures the peak delivery days in a month where completions are highest.
public record TopDeliveryDayDto(
        int rank,
        LocalDate date,
        String formattedDate,
        long orderCount,
        long itemCount,
        String tagColor
) {}
