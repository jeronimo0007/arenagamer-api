package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.TeamAvailabilityChangeRequest;
import com.arenagamer.api.entity.TeamAvailabilityRequestSlot;
import com.arenagamer.api.entity.enums.AvailabilityChangeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitação do capitão para alterar horários do time")
public class TeamAvailabilityChangeResponse {

    private Long id;
    private Long teamId;
    private Integer requestedByContactId;
    private String requestedByName;
    private AvailabilityChangeStatus status;
    private String message;
    private List<WeeklyAvailabilitySlotResponse> weeklySlots;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public static TeamAvailabilityChangeResponse from(TeamAvailabilityChangeRequest request) {
        List<WeeklyAvailabilitySlotResponse> slots = request.getSlots().stream()
                .map(TeamAvailabilityChangeResponse::toSlotResponse)
                .toList();
        String requesterName = null;
        if (request.getRequestedBy() != null) {
            String first = request.getRequestedBy().getFirstname() != null
                    ? request.getRequestedBy().getFirstname() : "";
            String last = request.getRequestedBy().getLastname() != null
                    ? " " + request.getRequestedBy().getLastname() : "";
            String combined = (first + last).trim();
            requesterName = combined.isEmpty() ? null : combined;
        }
        return TeamAvailabilityChangeResponse.builder()
                .id(request.getId())
                .teamId(request.getTeam().getId())
                .requestedByContactId(request.getRequestedBy().getId())
                .requestedByName(requesterName)
                .status(request.getStatus())
                .message(request.getMessage())
                .weeklySlots(slots)
                .createdAt(request.getCreatedAt())
                .resolvedAt(request.getResolvedAt())
                .build();
    }

    private static WeeklyAvailabilitySlotResponse toSlotResponse(TeamAvailabilityRequestSlot slot) {
        return WeeklyAvailabilitySlotResponse.builder()
                .dayOfWeek(slot.getDayOfWeek())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .build();
    }
}
