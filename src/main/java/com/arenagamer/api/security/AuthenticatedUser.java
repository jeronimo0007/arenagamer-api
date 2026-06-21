package com.arenagamer.api.security;

import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.Staff;
import com.arenagamer.api.entity.enums.AuthUserType;
import com.arenagamer.api.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticatedUser {

    private Long id;
    private AuthUserType type;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String avatarUrl;
    private String instagramUrl;
    private String youtubeUrl;
    private String twitchUrl;
    private UserRole role;
    private Boolean active;
    private Boolean emailVerified;
    private Integer clientUserId;
    private Boolean isPrimary;
    private Boolean walletViewAllowed;
    private Boolean walletUseAllowed;

    public static AuthenticatedUser fromStaff(Staff staff) {
        return AuthenticatedUser.builder()
                .id(staff.getStaffId().longValue())
                .type(AuthUserType.STAFF)
                .email(staff.getEmail())
                .firstName(staff.getFirstname())
                .lastName(staff.getLastname())
                .phoneNumber(staff.getPhonenumber())
                .avatarUrl(staff.getProfileImage())
                .role(staff.getAdmin() != null && staff.getAdmin() == 1 ? UserRole.ADMIN : UserRole.MANAGER)
                .active(staff.getActive() != null && staff.getActive() == 1)
                .emailVerified(true)
                .build();
    }

    public static AuthenticatedUser fromContact(Contact contact) {
        return AuthenticatedUser.builder()
                .id(contact.getId().longValue())
                .type(AuthUserType.CONTACT)
                .clientUserId(contact.getUserid())
                .email(contact.getEmail())
                .firstName(contact.getFirstname())
                .lastName(contact.getLastname())
                .phoneNumber(contact.getPhonenumber())
                .avatarUrl(contact.getProfileImage())
                .instagramUrl(contact.getInstagramUrl())
                .youtubeUrl(contact.getYoutubeUrl())
                .twitchUrl(contact.getTwitchUrl())
                .role(UserRole.PLAYER)
                .active(contact.getActive() != null && contact.getActive())
                .emailVerified(contact.getEmailVerifiedAt() != null)
                .isPrimary(contact.getIsPrimary() != null && contact.getIsPrimary() == 1)
                .walletViewAllowed(contact.getWalletViewAllowed())
                .walletUseAllowed(contact.getWalletUseAllowed())
                .build();
    }

    public boolean isStaff() {
        return type == AuthUserType.STAFF;
    }

    public boolean isContact() {
        return type == AuthUserType.CONTACT;
    }
}
