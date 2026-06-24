package com.arenagamer.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@Schema(description = "Um intervalo em um dia da semana. Vários por dia são permitidos se não colidirem.")
public class WeeklyAvailabilitySlotRequest {

    @NotNull
    @Schema(description = "Dia da semana: MONDAY, TUESDAY, ...")
    private DayOfWeek dayOfWeek;

    @NotNull
    @Schema(description = "Início do intervalo (HH:mm)", example = "14:00")
    private LocalTime startTime;

    @NotNull
    @Schema(description = "Fim do intervalo (HH:mm)", example = "18:00")
    private LocalTime endTime;
}
