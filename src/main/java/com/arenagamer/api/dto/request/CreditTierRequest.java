package com.arenagamer.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Dados para criar ou atualizar um tier de créditos")
public class CreditTierRequest {

    @NotNull @Min(2)
    @Schema(example = "2", description = "Mínimo de participantes da faixa")
    private Integer minParticipants;

    @NotNull @Min(2)
    @Schema(example = "8", description = "Máximo de participantes da faixa")
    private Integer maxParticipants;

    @NotNull @DecimalMin("0.00")
    @Schema(example = "5.00", description = "Custo em créditos para criar torneio nesta faixa")
    private BigDecimal creditCost;
}
