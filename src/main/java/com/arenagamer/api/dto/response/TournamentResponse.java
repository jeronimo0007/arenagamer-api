package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.Tournament;
import com.arenagamer.api.entity.enums.*;
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
    private BigDecimal entryFeeCredits;
    private BigDecimal feePercentage;
    private BigDecimal prizePool;
    private PrizeType prizeType;
    private Integer groupsCount;
    private Integer bestOf;
    private Long presetId;
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
    private LocalDateTime createdAt;

    public static TournamentResponse from(Tournament t) {
        return from(t, 0);
    }

    public static TournamentResponse from(Tournament t, int participantCount) {
        String presetIcon = t.getPreset() != null ? t.getPreset().getIconUrl() : null;
        String resolvedGameImage = resolveGameImageUrl(t.getGameImageUrl(), presetIcon);

        return TournamentResponse.builder()
                .id(t.getId())
                .slug(t.getSlug())
                .name(t.getName())
                .gameName(t.getGameName())
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
                .entryFeeCredits(t.getEntryFeeCredits())
                .feePercentage(t.getFeePercentage())
                .prizePool(t.getPrizePool())
                .prizeType(t.getPrizeType())
                .groupsCount(t.getGroupsCount())
                .bestOf(t.getBestOf())
                .presetId(t.getPreset() != null ? t.getPreset().getId() : null)
                .presetName(t.getPreset() != null ? t.getPreset().getGameName() : null)
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
