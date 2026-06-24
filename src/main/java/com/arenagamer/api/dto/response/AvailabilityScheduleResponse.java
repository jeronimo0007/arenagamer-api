package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.AvailabilityProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Agenda de disponibilidade para jogos")
public class AvailabilityScheduleResponse {

    private List<WeeklyAvailabilitySlotResponse> weeklySlots;

    public static AvailabilityScheduleResponse from(AvailabilityProfile profile) {
        if (profile == null) {
            return AvailabilityScheduleResponse.builder()
                    .weeklySlots(List.of())
                    .build();
        }
        return AvailabilityScheduleResponse.builder()
                .weeklySlots(WeeklyAvailabilitySlotResponse.fromSlots(profile.getWeeklySlots()))
                .build();
    }
}
