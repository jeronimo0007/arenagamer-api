package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.TeamRosterVacancy;
import com.arenagamer.api.entity.enums.TeamRosterVacancyStatus;
import com.arenagamer.api.entity.enums.TournamentStatus;
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
public class TeamRosterVacancyResponse {

    private Long id;
    private Long teamId;
    private Long tournamentId;
    private String tournamentSlug;
    private String tournamentName;
    private TournamentStatus tournamentStatus;
    private Long participantId;
    private Integer exitedClientUserId;
    private String exitedClientName;
    private Integer replacementClientUserId;
    private String replacementClientName;
    private TeamRosterVacancyStatus status;
    private LocalDateTime openedAt;
    private LocalDateTime resolvedAt;

    public static TeamRosterVacancyResponse from(TeamRosterVacancy vacancy) {
        return TeamRosterVacancyResponse.builder()
                .id(vacancy.getId())
                .teamId(vacancy.getTeam().getId())
                .tournamentId(vacancy.getTournament().getId())
                .tournamentSlug(vacancy.getTournament().getSlug())
                .tournamentName(vacancy.getTournament().getName())
                .tournamentStatus(vacancy.getTournament().getStatus())
                .participantId(vacancy.getParticipant().getId())
                .exitedClientUserId(vacancy.getExitedClient().getUserId())
                .exitedClientName(resolveClientName(vacancy.getExitedClient()))
                .replacementClientUserId(
                        vacancy.getReplacementClient() != null ? vacancy.getReplacementClient().getUserId() : null)
                .replacementClientName(
                        vacancy.getReplacementClient() != null
                                ? resolveClientName(vacancy.getReplacementClient())
                                : null)
                .status(vacancy.getStatus())
                .openedAt(vacancy.getOpenedAt())
                .resolvedAt(vacancy.getResolvedAt())
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
}
