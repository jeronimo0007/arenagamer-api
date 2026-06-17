package com.arenagamer.api.dto.request;

import com.arenagamer.api.entity.enums.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateTournamentRequest {

    @NotBlank @Size(max = 200)
    private String name;

    private String description;

    @NotNull
    private TournamentType type;

    @NotNull
    private TournamentFormat format;

    private Visibility visibility = Visibility.PUBLIC;

    @NotNull @Min(2)
    private Integer participantsLimit;

    private Integer minParticipants;

    private Long presetId;

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
}
