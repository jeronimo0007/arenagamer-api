package com.arenagamer.api.entity;

import com.arenagamer.api.entity.enums.AuthUserType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tblarena_refresh_tokens", indexes = {
    @Index(name = "idx_arena_refresh_token", columnList = "refresh_token", unique = true),
    @Index(name = "idx_arena_refresh_user", columnList = "user_type, user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArenaRefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 10)
    private AuthUserType userType;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "refresh_token", nullable = false, length = 512)
    private String refreshToken;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
