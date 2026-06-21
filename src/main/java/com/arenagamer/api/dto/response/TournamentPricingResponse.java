package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.TournamentPricingSettings;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Preços para criação de torneios")
public class TournamentPricingResponse {

    private BigDecimal baseTournamentPrice;
    private BigDecimal extraParticipantPrice;
    private Integer includedParticipants;

    public static TournamentPricingResponse from(TournamentPricingSettings settings) {
        return TournamentPricingResponse.builder()
                .baseTournamentPrice(settings.getBaseTournamentPrice())
                .extraParticipantPrice(settings.getExtraParticipantPrice())
                .includedParticipants(settings.getIncludedParticipants())
                .build();
    }
}
