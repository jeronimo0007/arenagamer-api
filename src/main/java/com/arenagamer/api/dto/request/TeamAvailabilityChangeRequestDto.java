package com.arenagamer.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class TeamAvailabilityChangeRequestDto {

    @NotEmpty
    @Valid
    @Schema(description = "Horários propostos para o time")
    private List<WeeklyAvailabilitySlotRequest> weeklySlots;

    @Size(max = 500)
    @Schema(description = "Mensagem opcional para o dono do time")
    private String message;
}
