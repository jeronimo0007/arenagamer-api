package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.Plan;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Plano de assinatura")
public class PlanResponse {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "Pro")
    private String name;

    @Schema(example = "Plano profissional para organizadores")
    private String description;

    @Schema(example = "10")
    private Integer freeTournamentsPerMonth;

    @Schema(example = "64")
    private Integer freeMaxParticipants;

    @Schema(example = "true")
    private Boolean allowsEntryFee;

    @Schema(example = "50")
    private Integer maxTournamentsPerMonth;

    @Schema(example = "29.90")
    private BigDecimal monthlyPrice;

    @Schema(example = "false")
    private Boolean hidden;

    @Schema(example = "true")
    private Boolean active;

    @Schema(example = "2")
    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static PlanResponse from(Plan plan) {
        return PlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .freeTournamentsPerMonth(plan.getFreeTournamentsPerMonth())
                .freeMaxParticipants(plan.getFreeMaxParticipants())
                .allowsEntryFee(plan.getAllowsEntryFee())
                .maxTournamentsPerMonth(plan.getMaxTournamentsPerMonth())
                .monthlyPrice(plan.getMonthlyPrice())
                .hidden(plan.getHidden())
                .active(plan.getActive())
                .sortOrder(plan.getSortOrder())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }
}
