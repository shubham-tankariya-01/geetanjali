package com.geetanjali.backend.dto.calendar;

import java.time.LocalDate;

// DayCapacityDto: Represents the capacity and overbooking status of a given delivery date.
// Standard limit: 5 orders per day.
// Max overbooking limit: 10 orders per day.
public record DayCapacityDto(
        LocalDate date,
        long orderCount,
        long itemCount,
        int standardLimit,
        int maxLimit,
        boolean available,
        boolean overbooking,
        boolean maxReached,
        String tagColor,
        String message
) {
    public static DayCapacityDto of(LocalDate date, long orderCount, long itemCount) {
        int standard = 5;
        int max = 10;
        boolean available = orderCount < standard;
        boolean overbooking = orderCount >= standard && orderCount < max;
        boolean maxReached = orderCount >= max;

        String color;
        if (orderCount == 0) {
            color = "NONE";
        } else if (orderCount < 4) {
            color = "GREEN";
        } else if (orderCount <= 5) {
            color = "YELLOW";
        } else {
            color = "RED";
        }

        String msg;
        if (maxReached) {
            msg = "Maximum daily capacity reached (" + orderCount + " orders). No more orders can be scheduled on this day.";
        } else if (overbooking) {
            msg = "This day already has " + orderCount + " orders scheduled (standard limit is " + standard + "). Confirm overbooking to add more.";
        } else {
            msg = "Available capacity (" + orderCount + " of " + standard + " standard orders booked).";
        }

        return new DayCapacityDto(
                date,
                orderCount,
                itemCount,
                standard,
                max,
                available,
                overbooking,
                maxReached,
                color,
                msg
        );
    }
}
