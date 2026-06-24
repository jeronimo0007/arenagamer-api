package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.Client;
import com.arenagamer.api.entity.Contact;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Jogador inscrito individualmente (solo)")
public class TournamentParticipantSoloResponse {

    private Integer clientUserId;
    private Integer contactId;
    private String nickname;
    private String firstName;
    private String lastName;
    private String profileImageUrl;

    public static TournamentParticipantSoloResponse from(Contact contact) {
        Client client = contact.getClient();
        return TournamentParticipantSoloResponse.builder()
                .clientUserId(contact.getUserid())
                .contactId(contact.getId())
                .nickname(client != null ? resolveNickname(client) : null)
                .firstName(contact.getFirstname())
                .lastName(contact.getLastname())
                .profileImageUrl(contact.getProfileImage())
                .build();
    }

    private static String resolveNickname(Client client) {
        if (client.getNickname() != null && !client.getNickname().isBlank()) {
            return client.getNickname();
        }
        return client.getCompany();
    }
}
