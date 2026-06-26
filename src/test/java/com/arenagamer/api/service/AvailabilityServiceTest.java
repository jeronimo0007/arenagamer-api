package com.arenagamer.api.service;

import com.arenagamer.api.dto.request.WeeklyAvailabilitySlotRequest;
import com.arenagamer.api.entity.AvailabilityProfile;
import com.arenagamer.api.entity.WeeklyAvailabilitySlot;
import com.arenagamer.api.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void findsPartialOverlapBetweenProfiles() {
        AvailabilityProfile a = profile(weeklySlot(DayOfWeek.SATURDAY, "14:00", "18:00"));
        AvailabilityProfile b = profile(weeklySlot(DayOfWeek.SATURDAY, "16:00", "20:00"));

        List<AvailabilityService.CommonSlot> common = availabilityService.findCommonWeeklySlots(a, b);

        assertThat(common).containsExactly(
                new AvailabilityService.CommonSlot(DayOfWeek.SATURDAY, LocalTime.of(16, 0), LocalTime.of(18, 0)));
    }

    @Test
    void returnsEmptyWhenNoOverlap() {
        AvailabilityProfile a = profile(weeklySlot(DayOfWeek.SATURDAY, "14:00", "16:00"));
        AvailabilityProfile b = profile(weeklySlot(DayOfWeek.SATURDAY, "17:00", "18:00"));

        assertThat(availabilityService.findCommonWeeklySlots(a, b)).isEmpty();
    }

    @Test
    void returnsEmptyForDifferentDays() {
        AvailabilityProfile a = profile(weeklySlot(DayOfWeek.MONDAY, "09:00", "11:00"));
        AvailabilityProfile b = profile(weeklySlot(DayOfWeek.TUESDAY, "09:00", "11:00"));

        assertThat(availabilityService.findCommonWeeklySlots(a, b)).isEmpty();
    }

    @Test
    void treatsNullProfileAsAlwaysAvailable() {
        AvailabilityProfile b = profile(weeklySlot(DayOfWeek.SATURDAY, "14:00", "18:00"));

        List<AvailabilityService.CommonSlot> common = availabilityService.findCommonWeeklySlots(null, b);

        assertThat(common).containsExactly(
                new AvailabilityService.CommonSlot(DayOfWeek.SATURDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)));
    }

    @Test
    void returnsEmptyWhenBothProfilesFree() {
        assertThat(availabilityService.findCommonWeeklySlots(null, new AvailabilityProfile())).isEmpty();
    }

    private static AvailabilityProfile profile(WeeklyAvailabilitySlot... slots) {
        AvailabilityProfile profile = new AvailabilityProfile();
        for (WeeklyAvailabilitySlot slot : slots) {
            slot.setAvailabilityProfile(profile);
            profile.getWeeklySlots().add(slot);
        }
        return profile;
    }

    private static WeeklyAvailabilitySlot weeklySlot(DayOfWeek day, String start, String end) {
        return WeeklyAvailabilitySlot.builder()
                .dayOfWeek(day)
                .startTime(LocalTime.parse(start))
                .endTime(LocalTime.parse(end))
                .build();
    }

    private static WeeklyAvailabilitySlotRequest slot(DayOfWeek day, String start, String end) {
        WeeklyAvailabilitySlotRequest request = new WeeklyAvailabilitySlotRequest();
        request.setDayOfWeek(day);
        request.setStartTime(LocalTime.parse(start));
        request.setEndTime(LocalTime.parse(end));
        return request;
    }
}
