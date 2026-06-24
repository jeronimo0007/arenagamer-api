package com.arenagamer.api.service;

import com.arenagamer.api.dto.request.WeeklyAvailabilitySlotRequest;
import com.arenagamer.api.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AvailabilityServiceTest {

    private final AvailabilityService availabilityService =
            new AvailabilityService(null);

    @Test
    void acceptsValidWeeklySlots() {
        assertDoesNotThrow(() -> availabilityService.validateWeeklySlots(List.of(
                slot(DayOfWeek.SATURDAY, "14:00", "18:00"),
                slot(DayOfWeek.SUNDAY, "10:00", "12:00"))));
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThrows(BusinessException.class, () -> availabilityService.validateWeeklySlots(List.of(
                slot(DayOfWeek.MONDAY, "18:00", "14:00"))));
    }

    @Test
    void acceptsMultipleNonOverlappingSlotsOnSameDay() {
        assertDoesNotThrow(() -> availabilityService.validateWeeklySlots(List.of(
                slot(DayOfWeek.SATURDAY, "10:00", "12:00"),
                slot(DayOfWeek.SATURDAY, "14:00", "18:00"),
                slot(DayOfWeek.SATURDAY, "20:00", "22:00"))));
    }

    @Test
    void acceptsAdjacentSlotsTouchingAtBoundary() {
        assertDoesNotThrow(() -> availabilityService.validateWeeklySlots(List.of(
                slot(DayOfWeek.MONDAY, "10:00", "12:00"),
                slot(DayOfWeek.MONDAY, "12:00", "14:00"))));
    }

    @Test
    void rejectsOverlappingSlotsOnSameDay() {
        assertThrows(BusinessException.class, () -> availabilityService.validateWeeklySlots(List.of(
                slot(DayOfWeek.FRIDAY, "10:00", "14:00"),
                slot(DayOfWeek.FRIDAY, "13:00", "16:00"))));
    }

    private static WeeklyAvailabilitySlotRequest slot(DayOfWeek day, String start, String end) {
        WeeklyAvailabilitySlotRequest request = new WeeklyAvailabilitySlotRequest();
        request.setDayOfWeek(day);
        request.setStartTime(LocalTime.parse(start));
        request.setEndTime(LocalTime.parse(end));
        return request;
    }
}
