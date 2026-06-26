package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.TeamJoinRequest;
import com.arenagamer.api.entity.enums.TeamJoinRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Convite para um cliente entrar no time")
public class TeamJoinRequestResponse {

    private Long id;
    private Long teamId;
    private String teamName;
    private String teamTag;
    private Integer invitedClientUserId;
    private String invitedClientName;
    private Integer invitedByContactId;
    private String invitedByName;
    private TeamJoinRequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public static TeamJoinRequestResponse from(TeamJoinRequest request) {
        return TeamJoinRequestResponse.builder()
                .id(request.getId())
                .teamId(request.getTeam().getId())
                .teamName(request.getTeam().getName())
                .teamTag(request.getTeam().getTag())
                .invitedClientUserId(request.getClient().getUserId())
                .invitedClientName(resolveClientName(request.getClient()))
                .invitedByContactId(request.getInvitedBy().getId())
                .invitedByName(resolveContactName(request.getInvitedBy()))
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .resolvedAt(request.getResolvedAt())
                .build();
    }

    private static String resolveClientName(com.arenagamer.api.entity.Client client) {
        if (client.getNickname() != null && !client.getNickname().isBlank()) {
            return client.getNickname().trim();
        }
        if (client.getCompany() != null && !client.getCompany().isBlank()) {
            return client.getCompany().trim();
        }
        return "Cliente #" + client.getUserId();
    }

    private static String resolveContactName(com.arenagamer.api.entity.Contact contact) {
        String first = contact.getFirstname() != null ? contact.getFirstname() : "";
        String last = contact.getLastname() != null ? " " + contact.getLastname() : "";
        String combined = (first + last).trim();
        return combined.isEmpty() ? null : combined;
    }
}
