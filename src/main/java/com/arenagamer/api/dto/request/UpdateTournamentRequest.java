package com.arenagamer.api.dto.request;

import com.arenagamer.api.entity.enums.*;
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

    @Size(max = 100)
    private String gameName;

    private String description;

    @Min(2)
    private Integer participantsLimit;

    private Integer minParticipants;

    private Long presetId;

    private Integer clientUserId;

    private TournamentType type;

    private TournamentFormat format;

    private Visibility visibility;

    private BigDecimal entryFeeCredits;

    private BigDecimal feePercentage;

    private PrizeType prizeType;

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
