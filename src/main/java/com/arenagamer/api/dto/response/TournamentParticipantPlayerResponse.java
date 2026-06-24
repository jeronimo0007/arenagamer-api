package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.Client;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Jogador escalado no torneio (time)")
public class TournamentParticipantPlayerResponse {

    private Integer clientUserId;
    private String nickname;

    public static TournamentParticipantPlayerResponse from(Client client) {
        return TournamentParticipantPlayerResponse.builder()
                .clientUserId(client.getUserId())
                .nickname(resolveNickname(client))
                .build();
    }

    private static String resolveNickname(Client client) {
        if (client.getNickname() != null && !client.getNickname().isBlank()) {
            return client.getNickname();
        }
        return client.getCompany();
    }
}
