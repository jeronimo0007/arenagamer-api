package com.arenagamer.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Contratação ou troca de plano")
public class SubscribePlanRequest {

    @NotNull
    @Schema(description = "ID do plano desejado", example = "1")
    private Long planId;

    @Min(1)
    @Max(12)
    @Schema(description = "Período de cobrança em meses (1=mensal, 6=semestral, 12=anual)", example = "1")
    private Integer billingPeriodMonths = 1;
}
