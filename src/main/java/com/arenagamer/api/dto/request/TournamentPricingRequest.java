package com.arenagamer.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Configuração de preço para criação de torneios")
public class TournamentPricingRequest {

    @NotNull
    @DecimalMin("0.00")
    @Schema(description = "Créditos cobrados pelo torneio padrão (inclui participantes padrão)", example = "5.00")
    private BigDecimal baseTournamentPrice;

    @NotNull
    @DecimalMin("0.00")
    @Schema(description = "Créditos por participante acima do limite incluído", example = "1.00")
    private BigDecimal extraParticipantPrice;

    @NotNull
    @Min(2)
    @Schema(description = "Quantidade de participantes incluídos no torneio padrão", example = "8")
    private Integer includedParticipants;
}
