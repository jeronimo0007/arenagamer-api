package com.arenagamer.api.entity;

import com.arenagamer.api.entity.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email", unique = true),
    @Index(name = "idx_users_username", columnList = "username", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.PLAYER;

    @Column(name = "perfex_client_id")
    private Long perfexClientId;

    @Column(name = "perfex_contact_id")
    private Long perfexContactId;

    @Column(name = "email_verified", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "phone_verified", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean phoneVerified = false;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    private Boolean active = true;

    @Column(length = 10)
    private String timezone;

    @Column(name = "refresh_token", length = 512)
    private String refreshToken;

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
