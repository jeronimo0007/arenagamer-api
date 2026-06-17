package com.arenagamer.api.service;

import com.arenagamer.api.dto.request.CreateTournamentRequest;
import com.arenagamer.api.dto.request.JoinTournamentRequest;
import com.arenagamer.api.entity.*;
import com.arenagamer.api.entity.enums.*;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PresetRepository presetRepository;
    private final WalletService walletService;
    private final CreditTierRepository creditTierRepository;
    private final AvailabilityProfileRepository availabilityProfileRepository;

    @Transactional
    public Tournament create(Long ownerId, CreateTournamentRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> BusinessException.notFound("Usuário não encontrado"));

        if (owner.getRole() == UserRole.PLAYER) {
            // Players may need credits to create tournaments
            var tier = creditTierRepository.findByParticipantCount(request.getParticipantsLimit());
            if (tier.isPresent()) {
                BigDecimal cost = tier.get().getCreditCost();
                walletService.holdCredits(ownerId, cost, "TOURNAMENT_CREATION", null);
            }
        }

        String slug = generateSlug(request.getName());

        Tournament tournament = Tournament.builder()
                .slug(slug)
                .name(request.getName())
                .description(request.getDescription())
                .owner(owner)
                .type(request.getType())
                .format(request.getFormat())
                .visibility(request.getVisibility() != null ? request.getVisibility() : Visibility.PUBLIC)
                .status(TournamentStatus.DRAFT)
                .participantsLimit(request.getParticipantsLimit())
                .minParticipants(request.getMinParticipants())
                .entryFeeCredits(request.getEntryFeeCredits() != null ? request.getEntryFeeCredits() : BigDecimal.ZERO)
                .feePercentage(request.getFeePercentage() != null ? request.getFeePercentage() : BigDecimal.ZERO)
                .prizeType(request.getPrizeType() != null ? request.getPrizeType() : PrizeType.MANUAL)
                .groupsCount(request.getGroupsCount())
                .teamsPerGroup(request.getTeamsPerGroup())
                .advancePerGroup(request.getAdvancePerGroup())
                .bestOf(request.getBestOf() != null ? request.getBestOf() : 1)
                .rules(request.getRules())
                .tiebreakerRules(request.getTiebreakerRules())
                .startDate(request.getStartDate())
                .registrationDeadline(request.getRegistrationDeadline())
                .build();

        if (request.getPresetId() != null) {
            Preset preset = presetRepository.findById(request.getPresetId())
                    .orElseThrow(() -> BusinessException.notFound("Preset não encontrado"));
            tournament.setPreset(preset);
        }

        return tournamentRepository.save(tournament);
    }

    public Tournament getBySlug(String slug) {
        return tournamentRepository.findBySlug(slug)
                .orElseThrow(() -> BusinessException.notFound("Torneio não encontrado"));
    }

    public Page<Tournament> listPublic(Pageable pageable) {
        return tournamentRepository.findByVisibilityAndStatusIn(
                Visibility.PUBLIC,
                List.of(TournamentStatus.REGISTRATION_OPEN, TournamentStatus.IN_PROGRESS, TournamentStatus.COMPLETED),
                pageable);
    }

    public Page<Tournament> listMyCreated(Long ownerId, Pageable pageable) {
        return tournamentRepository.findByOwnerId(ownerId, pageable);
    }

    public Page<Tournament> listMyJoined(Long userId, Pageable pageable) {
        return tournamentRepository.findJoinedByUserId(userId, pageable);
    }

    @Transactional
    public Tournament updateStatus(String slug, TournamentStatus newStatus, Long userId) {
        Tournament tournament = getBySlug(slug);
        validateOwnerOrAdmin(tournament, userId);
        tournament.setStatus(newStatus);
        return tournamentRepository.save(tournament);
    }

    @Transactional
    public TournamentParticipant joinSolo(String slug, Long userId, JoinTournamentRequest request) {
        Tournament tournament = getBySlug(slug);
        validateRegistrationOpen(tournament);

        if (participantRepository.existsByTournamentIdAndUserId(tournament.getId(), userId)) {
            throw BusinessException.conflict("Já inscrito neste torneio");
        }

        if (tournament.getFormat() != TournamentFormat.SOLO) {
            throw BusinessException.badRequest("Torneio não é solo, use inscrição por time");
        }

        long currentCount = participantRepository.countByTournamentIdAndStatus(tournament.getId(), ParticipantStatus.APPROVED);
        if (currentCount >= tournament.getParticipantsLimit()) {
            throw BusinessException.badRequest("Torneio lotado");
        }

        // Anti-fraud: owner cannot participate
        if (tournament.getOwner().getId().equals(userId)) {
            throw BusinessException.forbidden("Organizador não pode participar como jogador");
        }

        // Charge entry fee if applicable
        if (tournament.getEntryFeeCredits().compareTo(BigDecimal.ZERO) > 0) {
            walletService.holdCredits(userId, tournament.getEntryFeeCredits(), "ENTRY_FEE", tournament.getId());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("Usuário não encontrado"));

        AvailabilityProfile profile = createAvailabilityProfile(user, null, request);

        TournamentParticipant participant = TournamentParticipant.builder()
                .tournament(tournament)
                .user(user)
                .status(ParticipantStatus.APPROVED)
                .availabilityProfile(profile)
                .build();

        return participantRepository.save(participant);
    }

    @Transactional
    public TournamentParticipant joinTeam(String slug, Long userId, JoinTournamentRequest request) {
        Tournament tournament = getBySlug(slug);
        validateRegistrationOpen(tournament);

        if (request.getTeamId() == null) {
            throw BusinessException.badRequest("ID do time é obrigatório");
        }

        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> BusinessException.notFound("Time não encontrado"));

        if (!team.getOwner().getId().equals(userId)) {
            throw BusinessException.forbidden("Apenas o líder do time pode inscrever");
        }

        if (participantRepository.existsByTournamentIdAndTeamId(tournament.getId(), team.getId())) {
            throw BusinessException.conflict("Time já inscrito neste torneio");
        }

        if (tournament.getFormat() != TournamentFormat.TEAM) {
            throw BusinessException.badRequest("Torneio não é por time");
        }

        // Anti-fraud: owner cannot participate
        if (tournament.getOwner().getId().equals(userId)) {
            throw BusinessException.forbidden("Organizador não pode participar como jogador");
        }

        long currentCount = participantRepository.countByTournamentIdAndStatus(tournament.getId(), ParticipantStatus.APPROVED);
        if (currentCount >= tournament.getParticipantsLimit()) {
            throw BusinessException.badRequest("Torneio lotado");
        }

        if (tournament.getEntryFeeCredits().compareTo(BigDecimal.ZERO) > 0) {
            walletService.holdCredits(userId, tournament.getEntryFeeCredits(), "ENTRY_FEE", tournament.getId());
        }

        AvailabilityProfile profile = createAvailabilityProfile(null, team, request);

        TournamentParticipant participant = TournamentParticipant.builder()
                .tournament(tournament)
                .team(team)
                .status(ParticipantStatus.APPROVED)
                .availabilityProfile(profile)
                .build();

        return participantRepository.save(participant);
    }

    @Transactional
    public void kickParticipant(String slug, Long participantId, Long userId) {
        Tournament tournament = getBySlug(slug);
        validateOwnerOrAdmin(tournament, userId);

        TournamentParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> BusinessException.notFound("Participante não encontrado"));

        participant.setStatus(ParticipantStatus.KICKED);
        participantRepository.save(participant);

        // Refund entry fee if held
        Long refundUserId = participant.getUser() != null ? participant.getUser().getId() :
                (participant.getTeam() != null ? participant.getTeam().getOwner().getId() : null);
        if (refundUserId != null && tournament.getEntryFeeCredits().compareTo(BigDecimal.ZERO) > 0) {
            walletService.releaseHold(refundUserId, "ENTRY_FEE", tournament.getId());
        }
    }

    public List<TournamentParticipant> getParticipants(String slug) {
        Tournament tournament = getBySlug(slug);
        return participantRepository.findByTournamentId(tournament.getId());
    }

    @Transactional
    public void deleteTournament(String slug, Long userId) {
        Tournament tournament = getBySlug(slug);
        validateOwnerOrAdmin(tournament, userId);

        if (tournament.getStatus() == TournamentStatus.IN_PROGRESS) {
            throw BusinessException.badRequest("Não é possível excluir torneio em andamento");
        }

        tournament.setStatus(TournamentStatus.CANCELLED);
        tournamentRepository.save(tournament);
    }

    private void validateRegistrationOpen(Tournament tournament) {
        if (tournament.getStatus() != TournamentStatus.REGISTRATION_OPEN) {
            throw BusinessException.badRequest("Inscrições não estão abertas");
        }
    }

    public void validateOwnership(String slug, Long userId) {
        Tournament tournament = getBySlug(slug);
        validateOwnerOrAdmin(tournament, userId);
    }

    private void validateOwnerOrAdmin(Tournament tournament, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("Usuário não encontrado"));
        if (!tournament.getOwner().getId().equals(userId) && user.getRole() != UserRole.ADMIN) {
            throw BusinessException.forbidden("Sem permissão para esta ação");
        }
    }

    private AvailabilityProfile createAvailabilityProfile(User user, Team team, JoinTournamentRequest request) {
        if (request.getAvailableWindows() == null || request.getAvailableWindows().isEmpty()) {
            return null;
        }

        AvailabilityProfile profile = AvailabilityProfile.builder()
                .user(user)
                .team(team)
                .windows(request.getAvailableWindows())
                .preferWeekends(request.getPreferWeekends() != null && request.getPreferWeekends())
                .build();

        return availabilityProfileRepository.save(profile);
    }

    private String generateSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String slug = Pattern.compile("[^\\p{ASCII}]").matcher(normalized).replaceAll("");
        slug = slug.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");

        String baseSlug = slug;
        int counter = 1;
        while (tournamentRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }
        return slug;
    }
}
