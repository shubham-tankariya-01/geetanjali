package com.geetanjali.backend.dto.calendar;

import java.math.BigDecimal;

// CalendarOrderItemDto: Compact summary of an order item scheduled on a calendar day.
public record CalendarOrderItemDto(
        Long itemId,
        Long orderId,
        Long orderNo,
        String customerName,
        String customerMobile,
        String garmentName,
        String status,
        BigDecimal price,
        BigDecimal orderBalance
) {}
