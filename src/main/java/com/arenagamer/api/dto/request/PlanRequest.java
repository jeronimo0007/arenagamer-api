package com.arenagamer.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Dados para criar ou atualizar um plano")
public class PlanRequest {

    @NotBlank @Size(max = 100)
    @Schema(example = "Pro", description = "Nome do plano")
    private String name;

    @Schema(example = "Plano profissional para organizadores")
    private String description;

    @NotNull @Min(0)
    @Schema(example = "10", description = "Torneios gratuitos por mês")
    private Integer freeTournamentsPerMonth;

    @NotNull @Min(0)
    @Schema(example = "64", description = "Máximo de participantes em torneios gratuitos")
    private Integer freeMaxParticipants;

    @Schema(example = "true", description = "Permite taxa de inscrição nos torneios")
    private Boolean allowsEntryFee;

    @Min(0)
    @Schema(example = "50", description = "Limite total de torneios por mês (opcional)")
    private Integer maxTournamentsPerMonth;

    @NotNull @DecimalMin("0.00")
    @Schema(example = "29.90", description = "Preço mensal")
    private BigDecimal monthlyPrice;

    @Schema(example = "false", description = "Ocultar plano na listagem pública")
    private Boolean hidden;

    @Schema(example = "true", description = "Plano ativo")
    private Boolean active;

    @Min(0)
    @Schema(example = "2", description = "Ordem de exibição")
    private Integer sortOrder;
}
