package com.arenagamer.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Atribuição manual de plano a um cliente (staff)")
public class AdminSubscriptionAssignRequest {

    @NotNull
    @Schema(description = "ID do plano", example = "2")
    private Long planId;

    @Schema(description = "Período em meses (1, 6 ou 12)", example = "1")
    private Integer billingPeriodMonths = 1;
}
