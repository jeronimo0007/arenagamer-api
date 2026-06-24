package com.arenagamer.api.entity;

import com.arenagamer.api.entity.enums.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbltournaments", indexes = {
    @Index(name = "idx_tournaments_slug", columnList = "slug", unique = true),
    @Index(name = "idx_tournaments_owner", columnList = "owner_type, owner_id"),
    @Index(name = "idx_tournaments_status", columnList = "status"),
    @Index(name = "idx_tournaments_visibility", columnList = "visibility"),
    @Index(name = "idx_tournaments_client", columnList = "client_userid")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "game_name", length = 100)
    private String gameName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 10)
    private AuthUserType ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_userid", referencedColumnName = "userid")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preset_id")
    private Preset preset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TournamentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TournamentFormat format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    @Builder.Default
    private TournamentStatus status = TournamentStatus.DRAFT;

    @Column(name = "participants_limit", nullable = false)
    private Integer participantsLimit;

    @Column(name = "min_participants")
    private Integer minParticipants;

    /** Mínimo de jogadores (clientes) por equipe — somente quando format = TEAM. */
    @Column(name = "min_players_per_team")
    private Integer minPlayersPerTeam;

    /** Máximo de jogadores (clientes) por equipe — somente quando format = TEAM. */
    @Column(name = "max_players_per_team")
    private Integer maxPlayersPerTeam;

    @Column(name = "entry_fee_credits", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal entryFeeCredits = BigDecimal.ZERO;

    @Column(name = "fee_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal feePercentage = BigDecimal.ZERO;

    @Column(name = "prize_pool", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal prizePool = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "prize_type", length = 10)
    @Builder.Default
    private PrizeType prizeType = PrizeType.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "prize_funding", length = 20, nullable = false)
    @Builder.Default
    private PrizeFunding prizeFunding = PrizeFunding.FIXED;

    @Column(name = "groups_count")
    private Integer groupsCount;

    @Column(name = "teams_per_group")
    private Integer teamsPerGroup;

    @Column(name = "advance_per_group")
    private Integer advancePerGroup;

    @Column(name = "best_of")
    @Builder.Default
    private Integer bestOf = 1;

    @Column(name = "rules", columnDefinition = "TEXT")
    private String rules;

    @Column(name = "tiebreaker_rules", columnDefinition = "TEXT")
    private String tiebreakerRules;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "registration_deadline")
    private LocalDateTime registrationDeadline;

    @Column(name = "registration_opens_at")
    private LocalDateTime registrationOpensAt;

    @Column(name = "expected_end_date")
    private LocalDateTime expectedEndDate;

    @Column(name = "game_image_url", length = 500)
    private String gameImageUrl;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "logo_image_url", length = 500)
    private String logoImageUrl;

    @Column(name = "youtube_url", length = 500)
    private String youtubeUrl;

    @Column(name = "twitch_url", length = 500)
    private String twitchUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
