package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.Contact;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactWalletPermissionResponse {

    private Integer contactId;
    private String contactName;
    private String contactEmail;
    private Boolean walletViewAllowed;
    private Boolean walletUseAllowed;

    public static ContactWalletPermissionResponse from(Contact contact) {
        return ContactWalletPermissionResponse.builder()
                .contactId(contact.getId())
                .contactName(contact.getFirstname() + " " + contact.getLastname())
                .contactEmail(contact.getEmail())
                .walletViewAllowed(contact.getWalletViewAllowed())
                .walletUseAllowed(contact.getWalletUseAllowed())
                .build();
    }
}
