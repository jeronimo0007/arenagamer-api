package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.CreditTier;
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
@Schema(description = "Tier de créditos por faixa de participantes")
public class CreditTierResponse {

    private Long id;
    private Integer minParticipants;
    private Integer maxParticipants;
    private BigDecimal creditCost;

    public static CreditTierResponse from(CreditTier tier) {
        return CreditTierResponse.builder()
                .id(tier.getId())
                .minParticipants(tier.getMinParticipants())
                .maxParticipants(tier.getMaxParticipants())
                .creditCost(tier.getCreditCost())
                .build();
    }
}
