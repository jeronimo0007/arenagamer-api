package com.arenagamer.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tblteams", indexes = {
    @Index(name = "idx_teams_owner", columnList = "owner_contact_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String tag;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "youtube_url", length = 500)
    private String youtubeUrl;

    @Column(name = "instagram_url", length = 500)
    private String instagramUrl;

    @Column(name = "twitch_url", length = 500)
    private String twitchUrl;

    @Column(name = "other_social_url", length = 500)
    private String otherSocialUrl;

    @Column(name = "rules_change", columnDefinition = "TEXT")
    private String rulesChange;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_contact_id", nullable = false)
    private Contact owner;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TeamMember> members = new ArrayList<>();

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    private Boolean active = true;

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
