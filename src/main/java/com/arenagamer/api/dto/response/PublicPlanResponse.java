package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.Plan;
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
@Schema(description = "Plano disponível para clientes")
public class PublicPlanResponse {

    private Long id;
    private String name;
    private String description;
    private Integer freeTournamentsPerMonth;
    private Integer freeMaxParticipants;
    private Boolean allowsEntryFee;
    private Integer maxTournamentsPerMonth;
    private BigDecimal monthlyPrice;
    private Integer sortOrder;

    public static PublicPlanResponse from(Plan plan) {
        return PublicPlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .freeTournamentsPerMonth(plan.getFreeTournamentsPerMonth())
                .freeMaxParticipants(plan.getFreeMaxParticipants())
                .allowsEntryFee(plan.getAllowsEntryFee())
                .maxTournamentsPerMonth(plan.getMaxTournamentsPerMonth())
                .monthlyPrice(plan.getMonthlyPrice())
                .sortOrder(plan.getSortOrder())
                .build();
    }
}
