package com.geetanjali.backend.controller;

import com.geetanjali.backend.dto.calendar.MonthCalendarDto;
import com.geetanjali.backend.service.CalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Clock;
import java.time.LocalDate;

// CalendarController: Serves the Google Calendar-style boxed delivery schedule page,
// month view navigation, capacity tags, and monthly metrics sidebar.
@Controller
@RequestMapping("/calendar")
public class CalendarController {

    private final CalendarService calendarService;
    private final Clock clock;

    @Autowired
    public CalendarController(CalendarService calendarService, Clock clock) {
        this.calendarService = calendarService;
        this.clock = clock;
    }

    // Handles GET /calendar: Renders full month schedule view.
    @GetMapping
    public String showCalendar(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            Model model) {

        LocalDate today = LocalDate.now(clock);
        int targetYear = (year != null && year >= 2000 && year <= 2100) ? year : today.getYear();
        int targetMonth = (month != null && month >= 1 && month <= 12) ? month : today.getMonthValue();

        MonthCalendarDto calendarDto = calendarService.getMonthCalendar(targetYear, targetMonth);

        model.addAttribute("calendar", calendarDto);
        model.addAttribute("targetYear", targetYear);
        model.addAttribute("targetMonth", targetMonth);
        model.addAttribute("today", today);
        model.addAttribute("pageTitle", "Delivery Calendar");
        model.addAttribute("currentUri", "/calendar");

        return "calendar/index";
    }
}
