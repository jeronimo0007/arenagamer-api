package com.arenagamer.api.entity;

import com.arenagamer.api.entity.enums.AuthUserType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tblapi_keys", indexes = {
    @Index(name = "idx_api_keys_key", columnList = "api_key", unique = true),
    @Index(name = "idx_api_keys_owner", columnList = "owner_type, owner_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 10)
    private AuthUserType ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "api_key", nullable = false, unique = true, length = 64)
    private String key;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String scopes;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
