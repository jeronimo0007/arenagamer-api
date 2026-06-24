package com.arenagamer.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tblclient_ranks", indexes = {
    @Index(name = "idx_client_ranks_client", columnList = "client_userid"),
    @Index(name = "idx_client_ranks_preset", columnList = "preset_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_client_rank_preset", columnNames = {"client_userid", "preset_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientRank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_userid", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preset_id", nullable = false)
    private Preset preset;

    @Column(name = "rank_points", nullable = false)
    @Builder.Default
    private Integer rankPoints = 0;

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
