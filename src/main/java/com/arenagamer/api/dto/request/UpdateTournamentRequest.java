package com.arenagamer.api.dto.request;

import com.arenagamer.api.entity.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UpdateTournamentRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    private String description;

    @Min(2)
    private Integer participantsLimit;

    private Integer minParticipants;

    @Min(1)
    @Schema(description = "Mínimo de jogadores por equipe — obrigatório quando format = TEAM")
    private Integer minPlayersPerTeam;

    @Min(1)
    @Schema(description = "Máximo de jogadores por equipe — obrigatório quando format = TEAM")
    private Integer maxPlayersPerTeam;

    @Schema(description = "Jogo predefinido (opcional) — ao alterar, atualiza nome, regras e mídia do preset")
    private Long presetId;

    @Size(max = 100)
    @Schema(description = "Nome do jogo manual — usado quando não há preset selecionado")
    private String gameName;

    private Integer clientUserId;

    private TournamentType type;

    private TournamentFormat format;

    private Visibility visibility;

    private BigDecimal entryFeeCredits;

    private BigDecimal feePercentage;

    private PrizeType prizeType;

    private PrizeFunding prizeFunding;

    @Schema(description = "Valor do prêmio fixo em créditos")
    private BigDecimal prizePool;

    private Integer groupsCount;

    private Integer teamsPerGroup;

    private Integer advancePerGroup;

    private Integer bestOf;

    private String rules;

    private String tiebreakerRules;

    private LocalDateTime startDate;

    private LocalDateTime registrationDeadline;

    private LocalDateTime registrationOpensAt;

    private LocalDateTime expectedEndDate;

    @Size(max = 500)
    private String gameImageUrl;

    @Size(max = 500)
    private String coverImageUrl;

    @Size(max = 500)
    private String logoImageUrl;

    @Size(max = 500)
    private String youtubeUrl;

    @Size(max = 500)
    private String twitchUrl;
}
