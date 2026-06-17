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
    private String description;
    private Long ownerId;
    private String ownerName;
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
    private String presetName;
    private String rules;
    private LocalDateTime startDate;
    private LocalDateTime registrationDeadline;
    private Integer participantCount;
    private LocalDateTime createdAt;

    public static TournamentResponse from(Tournament t) {
        return TournamentResponse.builder()
                .id(t.getId())
                .slug(t.getSlug())
                .name(t.getName())
                .description(t.getDescription())
                .ownerId(t.getOwner().getId())
                .ownerName(t.getOwner().getFirstName() + " " + t.getOwner().getLastName())
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
                .presetName(t.getPreset() != null ? t.getPreset().getGameName() : null)
                .rules(t.getRules())
                .startDate(t.getStartDate())
                .registrationDeadline(t.getRegistrationDeadline())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
