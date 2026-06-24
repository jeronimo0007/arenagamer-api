package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.Tournament;
import com.arenagamer.api.entity.enums.*;
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
public class TournamentResponse {

    private Long id;
    private String slug;
    private String name;
    @Schema(description = "Nome do jogo (derivado do preset)")
    private String gameName;
    private String description;
    private Long ownerId;
    private AuthUserType ownerType;
    private String ownerName;
    private Integer clientUserId;
    private TournamentType type;
    private TournamentFormat format;
    private Visibility visibility;
    private TournamentStatus status;
    private Integer participantsLimit;
    private Integer minParticipants;
    @Schema(description = "Mínimo de jogadores por equipe (somente format = TEAM)")
    private Integer minPlayersPerTeam;
    @Schema(description = "Máximo de jogadores por equipe (somente format = TEAM)")
    private Integer maxPlayersPerTeam;
    private BigDecimal entryFeeCredits;
    private BigDecimal feePercentage;
    private BigDecimal prizePool;
    private PrizeType prizeType;
    private PrizeFunding prizeFunding;
    private Integer groupsCount;
    private Integer bestOf;
    private Long presetId;
    @Schema(description = "Nome do jogo (igual a gameName quando há preset)")
    private String presetName;
    private String presetIconUrl;
    private String rules;
    private LocalDateTime startDate;
    private LocalDateTime registrationDeadline;
    private LocalDateTime registrationOpensAt;
    private LocalDateTime expectedEndDate;
    private String gameImageUrl;
    private String coverImageUrl;
    private String logoImageUrl;
    private String youtubeUrl;
    private String twitchUrl;
    private Integer participantCount;
    @Schema(description = "Total arrecadado em taxas de inscrição (somente prizeFunding = ENTRY_FEES)")
    private BigDecimal collectedEntryFeeCredits;
    private LocalDateTime createdAt;

    public static TournamentResponse from(Tournament t) {
        return from(t, 0);
    }

    public static TournamentResponse from(Tournament t, int participantCount) {
        String presetIcon = t.getPreset() != null ? t.getPreset().getIconUrl() : null;
        String resolvedGameImage = resolveGameImageUrl(t.getGameImageUrl(), presetIcon);
        String gameName = resolveGameName(t);

        return TournamentResponse.builder()
                .id(t.getId())
                .slug(t.getSlug())
                .name(t.getName())
                .gameName(gameName)
                .description(t.getDescription())
                .ownerId(t.getOwnerId())
                .ownerType(t.getOwnerType())
                .ownerName(null)
                .clientUserId(t.getClient() != null ? t.getClient().getUserId() : null)
                .type(t.getType())
                .format(t.getFormat())
                .visibility(t.getVisibility())
                .status(t.getStatus())
                .participantsLimit(t.getParticipantsLimit())
                .minParticipants(t.getMinParticipants())
                .minPlayersPerTeam(t.getMinPlayersPerTeam())
                .maxPlayersPerTeam(t.getMaxPlayersPerTeam())
                .entryFeeCredits(t.getEntryFeeCredits())
                .feePercentage(t.getFeePercentage())
                .prizePool(t.getPrizePool())
                .prizeType(t.getPrizeType())
                .prizeFunding(t.getPrizeFunding())
                .groupsCount(t.getGroupsCount())
                .bestOf(t.getBestOf())
                .presetId(t.getPreset() != null ? t.getPreset().getId() : null)
                .presetName(gameName)
                .presetIconUrl(presetIcon)
                .rules(t.getRules())
                .startDate(t.getStartDate())
                .registrationDeadline(t.getRegistrationDeadline())
                .registrationOpensAt(t.getRegistrationOpensAt())
                .expectedEndDate(t.getExpectedEndDate())
                .gameImageUrl(resolvedGameImage)
                .coverImageUrl(t.getCoverImageUrl())
                .logoImageUrl(t.getLogoImageUrl())
                .youtubeUrl(t.getYoutubeUrl())
                .twitchUrl(t.getTwitchUrl())
                .participantCount(participantCount)
                .createdAt(t.getCreatedAt())
                .build();
    }

    private static String resolveGameName(Tournament t) {
        if (t.getPreset() != null && t.getPreset().getGameName() != null && !t.getPreset().getGameName().isBlank()) {
            return t.getPreset().getGameName().trim();
        }
        if (t.getGameName() != null && !t.getGameName().isBlank()) {
            return t.getGameName().trim();
        }
        return null;
    }

    private static String resolveGameImageUrl(String gameImageUrl, String presetIconUrl) {
        if (gameImageUrl != null && !gameImageUrl.isBlank()) {
            return gameImageUrl.trim();
        }
        if (presetIconUrl != null && !presetIconUrl.isBlank()) {
            return presetIconUrl.trim();
        }
        return null;
    }
}
