package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.enums.AuthUserType;
import com.arenagamer.api.entity.enums.UserRole;
import com.arenagamer.api.entity.enums.Visibility;
import com.arenagamer.api.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private AuthUserType userType;
    private String email;
    private String firstName;
    private String lastName;
    private String nickname;
    @Schema(description = "PUBLIC, PRIVATE ou PROTECTED")
    private Visibility privacy;
    private List<TeamRankSummaryResponse> ranks;
    private AvailabilityScheduleResponse availability;
    private String phoneNumber;
    private String avatarUrl;
    private String instagramUrl;
    private String youtubeUrl;
    private String twitchUrl;
    private UserRole role;
    private Boolean emailVerified;
    private Integer clientUserId;
    private Boolean isPrimary;
    private Boolean walletViewAllowed;
    private Boolean walletUseAllowed;
    private Boolean canViewWallet;
    private Boolean canUseWallet;
    private UserPlanResponse plan;

    public static UserResponse from(AuthenticatedUser user) {
        return from(user, null);
    }

    public static UserResponse from(AuthenticatedUser user, UserPlanResponse plan) {
        Boolean isPrimary = user.isContact() ? user.getIsPrimary() : null;
        boolean primary = Boolean.TRUE.equals(isPrimary);
        boolean canView = user.isContact() && (primary || Boolean.TRUE.equals(user.getWalletViewAllowed()));
        boolean canUse = user.isContact() && (primary || Boolean.TRUE.equals(user.getWalletUseAllowed()));

        return UserResponse.builder()
                .id(user.getId())
                .userType(user.getType())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .instagramUrl(user.getInstagramUrl())
                .youtubeUrl(user.getYoutubeUrl())
                .twitchUrl(user.getTwitchUrl())
                .role(user.getRole())
                .emailVerified(user.getEmailVerified())
                .clientUserId(user.getClientUserId())
                .isPrimary(isPrimary)
                .walletViewAllowed(user.isContact() ? user.getWalletViewAllowed() : null)
                .walletUseAllowed(user.isContact() ? user.getWalletUseAllowed() : null)
                .canViewWallet(user.isContact() ? canView : null)
                .canUseWallet(user.isContact() ? canUse : null)
                .plan(user.isContact() ? plan : null)
                .build();
    }
}
