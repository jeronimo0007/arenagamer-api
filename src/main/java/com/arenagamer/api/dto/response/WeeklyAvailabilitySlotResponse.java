package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.WeeklyAvailabilitySlot;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Intervalo recorrente em um dia da semana")
public class WeeklyAvailabilitySlotResponse {

    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;

    public static WeeklyAvailabilitySlotResponse from(WeeklyAvailabilitySlot slot) {
        return WeeklyAvailabilitySlotResponse.builder()
                .dayOfWeek(slot.getDayOfWeek())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .build();
    }

    public static List<WeeklyAvailabilitySlotResponse> fromSlots(List<WeeklyAvailabilitySlot> slots) {
        if (slots == null || slots.isEmpty()) {
            return List.of();
        }
        return slots.stream()
                .map(WeeklyAvailabilitySlotResponse::from)
                .sorted(Comparator.comparing(WeeklyAvailabilitySlotResponse::getDayOfWeek)
                        .thenComparing(WeeklyAvailabilitySlotResponse::getStartTime))
                .toList();
    }
}
