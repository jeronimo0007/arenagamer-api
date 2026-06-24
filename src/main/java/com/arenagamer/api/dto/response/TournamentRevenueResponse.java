package com.arenagamer.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentRevenueResponse {

    private Long tournamentId;
    private String tournamentSlug;
    private String tournamentName;

    @Schema(description = "Taxa de inscrição por participante")
    private BigDecimal entryFeeCredits;

    @Schema(description = "Total arrecadado (taxas retidas e ainda não reembolsadas)")
    private BigDecimal collectedCredits;

    @Schema(description = "Total já capturado definitivamente (torneio iniciado)")
    private BigDecimal capturedCredits;

    @Schema(description = "Total reembolsado a participantes")
    private BigDecimal refundedCredits;

    @Schema(description = "Inscrições com taxa paga — identifica time ou jogador inscrito e quem pagou")
    private List<TournamentEntryFeeItemResponse> entries;
}
