package com.arenagamer.api.service;

import com.arenagamer.api.dto.response.AvailabilityScheduleResponse;
import com.arenagamer.api.dto.response.PlayerDetailResponse;
import com.arenagamer.api.dto.response.TeamActiveTournamentResponse;
import com.arenagamer.api.dto.response.TeamDetailAccess;
import com.arenagamer.api.dto.response.TeamPerformanceResponse;
import com.arenagamer.api.dto.response.TeamRankSummaryResponse;
import com.arenagamer.api.entity.Client;
import com.arenagamer.api.entity.ClientRank;
import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.enums.ParticipantStatus;
import com.arenagamer.api.entity.enums.TeamStatus;
import com.arenagamer.api.entity.enums.TournamentStatus;
import com.arenagamer.api.entity.enums.Visibility;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.ClientRankRepository;
import com.arenagamer.api.repository.ClientRepository;
import com.arenagamer.api.repository.ContactRepository;
import com.arenagamer.api.repository.PresetRepository;
import com.arenagamer.api.repository.TournamentParticipantRepository;
import com.arenagamer.api.security.AuthenticatedUser;
import com.arenagamer.api.util.TeamVisibilityRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlayerProfileService {

    private final ClientRepository clientRepository;
    private final ClientRankRepository clientRankRepository;
    private final ContactRepository contactRepository;
    private final PresetRepository presetRepository;
    private final TournamentParticipantRepository tournamentParticipantRepository;
    private final ClientRankService clientRankService;
    private final AvailabilityService availabilityService;

    private static final List<TournamentStatus> ACTIVE_TOURNAMENT_STATUSES = List.of(
            TournamentStatus.REGISTRATION_OPEN,
            TournamentStatus.REGISTRATION_CLOSED,
            TournamentStatus.IN_PROGRESS);

    @Transactional(readOnly = true)
    public PlayerDetailResponse getPlayerDetails(Integer clientUserId, AuthenticatedUser auth, Long presetId) {
        Client client = getActiveClient(clientUserId);
        validateOptionalPreset(presetId);

        boolean isOwner = isOwnerClient(client, auth);
        TeamDetailAccess access = resolveDetailAccess(client.getVisibility(), isOwner);

        if (access == TeamDetailAccess.PRIVATE_RESTRICTED) {
            return PlayerDetailResponse.privateRestricted();
        }
        if (access == TeamDetailAccess.PROTECTED_SUMMARY) {
            return buildProtectedSummary(client, presetId);
        }
        return buildFullDetails(client, presetId);
    }

    @Transactional(readOnly = true)
    public PlayerDetailResponse getDiscoverablePlayerDetails(
            Integer clientUserId, AuthenticatedUser auth, Long presetId) {
        Client client = clientRepository.findDiscoverableById(clientUserId, TeamVisibilityRules.DISCOVERABLE)
                .orElseGet(() -> {
                    Client existing = clientRepository.findById(clientUserId).orElse(null);
                    if (existing != null && existing.getVisibility() == Visibility.PRIVATE) {
                        return null;
                    }
                    throw BusinessException.notFound("Jogador não encontrado");
                });

        if (client == null) {
            return PlayerDetailResponse.privateRestricted();
        }

        validateOptionalPreset(presetId);
        boolean isOwner = isOwnerClient(client, auth);
        TeamDetailAccess access = resolveDetailAccess(client.getVisibility(), isOwner);

        if (access == TeamDetailAccess.PROTECTED_SUMMARY) {
            return buildProtectedSummary(client, presetId);
        }
        return buildFullDetails(client, presetId);
    }

    private PlayerDetailResponse buildFullDetails(Client client, Long presetId) {
        List<ClientRank> ranks = clientRankRepository.findByClientUserIdWithPreset(client.getUserId());
        Optional<ClientRank> displayRank = resolveDisplayRank(client.getUserId(), presetId, ranks);
        List<TeamPerformanceResponse> performance = ranks.stream()
                .map(clientRankService::toPerformance)
                .toList();

        var activeTournaments = tournamentParticipantRepository
                .findActiveByClientUserId(client.getUserId(), ParticipantStatus.APPROVED, ACTIVE_TOURNAMENT_STATUSES)
                .stream()
                .map(TeamActiveTournamentResponse::from)
                .toList();

        return PlayerDetailResponse.builder()
                .access(TeamDetailAccess.FULL)
                .clientUserId(client.getUserId())
                .nickname(resolveNickname(client))
                .profileImageUrl(resolveProfileImage(client.getUserId()))
                .privacy(client.getVisibility())
                .rank(displayRank.map(this::toRankSummary).orElse(null))
                .performance(performance.isEmpty() ? null : performance)
                .activeTournaments(activeTournaments.isEmpty() ? null : activeTournaments)
                .availability(availabilityService.getClientSchedule(client.getUserId()))
                .status(resolveStatus(client, !activeTournaments.isEmpty()))
                .build();
    }

    private PlayerDetailResponse buildProtectedSummary(Client client, Long presetId) {
        List<ClientRank> ranks = clientRankRepository.findByClientUserIdWithPreset(client.getUserId());
        Optional<ClientRank> displayRank = resolveDisplayRank(client.getUserId(), presetId, ranks);
        boolean inTournament = !tournamentParticipantRepository
                .findActiveByClientUserId(client.getUserId(), ParticipantStatus.APPROVED, ACTIVE_TOURNAMENT_STATUSES)
                .isEmpty();

        return PlayerDetailResponse.builder()
                .access(TeamDetailAccess.PROTECTED_SUMMARY)
                .nickname(resolveNickname(client))
                .rank(displayRank.map(this::toRankSummary).orElse(null))
                .status(resolveStatus(client, inTournament))
                .build();
    }

    private TeamRankSummaryResponse toRankSummary(ClientRank rank) {
        return TeamRankSummaryResponse.builder()
                .presetId(rank.getPreset().getId())
                .gameName(rank.getPreset().getGameName())
                .platform(rank.getPreset().getPlatform())
                .rankPoints(rank.getRankPoints())
                .build();
    }

    private Optional<ClientRank> resolveDisplayRank(
            Integer clientUserId, Long presetId, List<ClientRank> ranks) {
        if (presetId != null) {
            return clientRankRepository.findByClientUserIdAndPresetIdWithDetails(clientUserId, presetId);
        }
        Long mostPlayedPresetId = resolveMostPlayedPresetId(clientUserId);
        if (mostPlayedPresetId != null) {
            Optional<ClientRank> mostPlayed = clientRankRepository.findByClientUserIdAndPresetIdWithDetails(
                    clientUserId, mostPlayedPresetId);
            if (mostPlayed.isPresent()) {
                return mostPlayed;
            }
        }
        return ranks.stream()
                .max(Comparator.comparing(ClientRank::getRankPoints)
                        .thenComparing(rank -> rank.getPreset().getId()));
    }

    private Long resolveMostPlayedPresetId(Integer clientUserId) {
        List<Object[]> rows = tournamentParticipantRepository.countApprovedParticipationsByPresetForClient(
                clientUserId, ParticipantStatus.APPROVED);
        if (rows.isEmpty() || rows.get(0)[0] == null) {
            return null;
        }
        Object presetIdValue = rows.get(0)[0];
        if (presetIdValue instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private TeamDetailAccess resolveDetailAccess(Visibility visibility, boolean isOwner) {
        return switch (visibility) {
            case PUBLIC -> TeamDetailAccess.FULL;
            case PRIVATE -> isOwner ? TeamDetailAccess.FULL : TeamDetailAccess.PRIVATE_RESTRICTED;
            case PROTECTED -> isOwner ? TeamDetailAccess.FULL : TeamDetailAccess.PROTECTED_SUMMARY;
        };
    }

    private boolean isOwnerClient(Client client, AuthenticatedUser auth) {
        return auth != null
                && auth.isContact()
                && auth.getClientUserId() != null
                && auth.getClientUserId().equals(client.getUserId());
    }

    private Client getActiveClient(Integer clientUserId) {
        Client client = clientRepository.findById(clientUserId)
                .orElseThrow(() -> BusinessException.notFound("Jogador não encontrado"));
        if (client.getActive() == null || client.getActive() != 1) {
            throw BusinessException.notFound("Jogador não encontrado");
        }
        return client;
    }

    private String resolveNickname(Client client) {
        if (client.getNickname() != null && !client.getNickname().isBlank()) {
            return client.getNickname();
        }
        return client.getCompany();
    }

    private String resolveProfileImage(Integer clientUserId) {
        return contactRepository.findByUseridAndIsPrimary(clientUserId, 1)
                .map(Contact::getProfileImage)
                .orElse(null);
    }

    private TeamStatus resolveStatus(Client client, boolean inActiveTournament) {
        if (client.getActive() == null || client.getActive() != 1) {
            return TeamStatus.INACTIVE;
        }
        if (inActiveTournament) {
            return TeamStatus.IN_TOURNAMENT;
        }
        return TeamStatus.ACTIVE;
    }

    private void validateOptionalPreset(Long presetId) {
        if (presetId == null) {
            return;
        }
        presetRepository.findByIdAndActiveTrue(presetId)
                .orElseThrow(() -> BusinessException.badRequest("Preset inválido ou inativo: " + presetId));
    }
}
