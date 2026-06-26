package com.arenagamer.api.service;

import com.arenagamer.api.entity.Client;
import com.arenagamer.api.entity.TeamJoinBan;
import com.arenagamer.api.entity.TeamRosterVacancy;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.ClientRepository;
import com.arenagamer.api.repository.TeamJoinBanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TeamJoinBanService {

    private final TeamJoinBanRepository banRepository;
    private final ClientRepository clientRepository;
    private final TeamSettingsService teamSettingsService;

    @Transactional(readOnly = true)
    public Optional<TeamJoinBan> findActiveBan(Integer clientUserId) {
        if (clientUserId == null) {
            return Optional.empty();
        }
        return banRepository.findActiveByClientUserId(clientUserId, LocalDateTime.now());
    }

    public void requireCanJoinTeam(Integer clientUserId) {
        findActiveBan(clientUserId).ifPresent(ban -> {
            throw BusinessException.forbidden(
                    "Você não pode entrar em um time até "
                            + ban.getBannedUntil().toLocalDate()
                            + ". Motivo: saída de equipe em torneio sem reposição.");
        });
    }

    @Transactional
    public TeamJoinBan applyBanForUnreplacedExit(Client client, TeamRosterVacancy vacancy) {
        int days = Math.max(1, teamSettingsService.getSettings().getTeamJoinBanDaysAfterUnreplacedExit());
        LocalDateTime bannedUntil = LocalDateTime.now().plusDays(days);

        TeamJoinBan ban = TeamJoinBan.builder()
                .client(client)
                .reason("Saída de time em torneio sem reposição na escalação")
                .bannedUntil(bannedUntil)
                .rosterVacancy(vacancy)
                .build();

        return banRepository.save(ban);
    }
}
