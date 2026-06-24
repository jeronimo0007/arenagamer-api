package com.arenagamer.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AvailabilityScheduleRequest {

    @Valid
    @Schema(description = """
            Substitui todos os horários. Permite vários intervalos no mesmo dia \
            (ex.: sábado 10:00–12:00 e 14:00–18:00), desde que não se sobreponham. \
            Intervalos que apenas se encostam são permitidos (ex.: 10:00–12:00 e 12:00–14:00).""")
    private List<WeeklyAvailabilitySlotRequest> weeklySlots;
}
