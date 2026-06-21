package com.arenagamer.api.entity;

import com.arenagamer.api.entity.enums.AuthUserType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tblwebhook_subscriptions", indexes = {
    @Index(name = "idx_webhook_owner", columnList = "owner_type, owner_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 10)
    private AuthUserType ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private String url;

    @Column(name = "event_types", nullable = false, columnDefinition = "JSON")
    private String eventTypes;

    @Column(length = 255)
    private String secret;

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
