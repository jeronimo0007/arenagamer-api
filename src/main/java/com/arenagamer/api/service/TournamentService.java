package com.arenagamer.api.service;

import com.arenagamer.api.dto.request.CreateTournamentRequest;
import com.arenagamer.api.dto.request.JoinTournamentRequest;
import com.arenagamer.api.dto.request.UpdateTournamentRequest;
import com.arenagamer.api.dto.response.TournamentManagerResponse;
import com.arenagamer.api.dto.response.TournamentResponse;
import com.arenagamer.api.entity.*;
import com.arenagamer.api.entity.enums.*;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.*;
import com.arenagamer.api.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentParticipantRepository participantRepository;
    private final TournamentManagerPermissionRepository managerPermissionRepository;
    private final TeamRepository teamRepository;
    private final PresetRepository presetRepository;
    private final ClientRepository clientRepository;
    private final ContactRepository contactRepository;
    private final WalletService walletService;
    private final AvailabilityProfileRepository availabilityProfileRepository;
    private final IdentityService identityService;
    private final TournamentAccessService tournamentAccessService;
    private final AuditService auditService;
    private final PlanEntitlementService planEntitlementService;
    private final TeamSettingsService teamSettingsService;

    @Transactional
    public Tournament create(AuthenticatedUser auth, CreateTournamentRequest request) {
        Contact contact = null;
        PlanEntitlementService.TournamentCreationEntitlement entitlement = null;

        if (!auth.isStaff()) {
            contact = identityService.requireContact(auth);
            if (request.getClientUserId() != null) {
                throw BusinessException.forbidden("Contatos não podem vincular torneio a um cliente");
            }

            entitlement = planEntitlementService.prepareTournamentCreation(contact.getUserid(), request);
            if (entitlement.creditCost().compareTo(BigDecimal.ZERO) > 0) {
                walletService.holdCredits(
                        contact.getUserid(),
                        contact,
                        entitlement.creditCost(),
                        "TOURNAMENT_CREATION",
                        null);
            }
        }

        String slug = generateSlug(request.getName());

        Tournament tournament = Tournament.builder()
                .slug(slug)
                .name(request.getName())
                .gameName(normalizeGameName(request.getGameName()))
                .description(request.getDescription())
                .ownerType(auth.getType())
                .ownerId(auth.getId())
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
                .registrationOpensAt(request.getRegistrationOpensAt())
                .expectedEndDate(request.getExpectedEndDate())
                .gameImageUrl(normalizeUrl(request.getGameImageUrl()))
                .coverImageUrl(normalizeUrl(request.getCoverImageUrl()))
                .logoImageUrl(normalizeUrl(request.getLogoImageUrl()))
                .youtubeUrl(normalizeUrl(request.getYoutubeUrl()))
                .twitchUrl(normalizeUrl(request.getTwitchUrl()))
                .build();

        if (request.getPresetId() != null) {
            Preset preset = presetRepository.findById(request.getPresetId())
                    .orElseThrow(() -> BusinessException.notFound("Preset não encontrado"));
            tournament.setPreset(preset);
            applyPresetSnapshot(tournament, preset);
        }

        resolveClientLink(auth, request, tournament);

        Tournament saved = tournamentRepository.save(tournament);
        if (entitlement != null) {
            planEntitlementService.recordTournamentCreated(entitlement.subscription());
        }
        auditStaff(auth, "CREATE", saved, "Torneio criado: " + saved.getSlug());
        return saved;
    }

    private void resolveClientLink(AuthenticatedUser auth, CreateTournamentRequest request, Tournament tournament) {
        if (auth.isStaff()) {
            if (request.getClientUserId() == null) {
                return;
            }
            Client client = clientRepository.findById(request.getClientUserId())
                    .orElseThrow(() -> BusinessException.notFound("Cliente não encontrado"));
            tournament.setClient(client);
            return;
        }

        Contact contact = identityService.requireContact(auth);
        Client client = clientRepository.findById(contact.getUserid())
                .orElseThrow(() -> BusinessException.notFound("Cliente não encontrado"));
        tournament.setClient(client);
    }

    public Tournament getBySlug(String slug) {
        return tournamentRepository.findBySlug(slug)
                .orElseThrow(() -> BusinessException.notFound("Torneio não encontrado"));
    }

    public Page<Tournament> listPublic(Pageable pageable) {
        return listPublic(null, pageable);
    }

    public Page<Tournament> listPublic(PublicTournamentFilter filter, Pageable pageable) {
        if (filter == null) {
            return tournamentRepository.findByVisibilityAndStatusIn(
                    Visibility.PUBLIC,
                    List.of(
                            TournamentStatus.REGISTRATION_OPEN,
                            TournamentStatus.IN_PROGRESS,
                            TournamentStatus.COMPLETED),
                    pageable);
        }

        return switch (filter) {
            case REGISTRATION_OPEN -> tournamentRepository.findByVisibilityAndStatus(
                    Visibility.PUBLIC, TournamentStatus.REGISTRATION_OPEN, pageable);
            case IN_PROGRESS -> tournamentRepository.findByVisibilityAndStatus(
                    Visibility.PUBLIC, TournamentStatus.IN_PROGRESS, pageable);
            case FINISHED -> tournamentRepository.findByVisibilityAndStatus(
                    Visibility.PUBLIC, TournamentStatus.COMPLETED, pageable);
            case CANCELLED -> tournamentRepository.findByVisibilityAndStatus(
                    Visibility.PUBLIC, TournamentStatus.CANCELLED, pageable);
            case UPCOMING -> tournamentRepository.findPublicWithRegistrationOpensAt(pageable);
        };
    }

    public TournamentResponse toResponse(Tournament tournament) {
        int participantCount = countApprovedParticipants(tournament.getId());
        return TournamentResponse.from(tournament, participantCount);
    }

    public Page<TournamentResponse> toResponsePage(Page<Tournament> page) {
        Map<Long, Integer> participantCounts = loadApprovedParticipantCounts(
                page.getContent().stream().map(Tournament::getId).toList());
        return page.map(tournament -> TournamentResponse.from(
                tournament,
                participantCounts.getOrDefault(tournament.getId(), 0)));
    }

    private int countApprovedParticipants(Long tournamentId) {
        return (int) participantRepository.countByTournamentIdAndStatus(
                tournamentId, ParticipantStatus.APPROVED);
    }

    private Map<Long, Integer> loadApprovedParticipantCounts(List<Long> tournamentIds) {
        if (tournamentIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Integer> counts = new HashMap<>();
        participantRepository.countByTournamentIdsAndStatus(tournamentIds, ParticipantStatus.APPROVED)
                .forEach(row -> counts.put((Long) row[0], ((Number) row[1]).intValue()));
        return counts;
    }

    @Transactional
    public Tournament update(String slug, AuthenticatedUser auth, UpdateTournamentRequest request) {
        Tournament tournament = getBySlug(slug);
        tournamentAccessService.validateCanManage(tournament, auth);
        validateEditable(tournament);

        if (request.getName() != null && !request.getName().isBlank()) {
            tournament.setName(request.getName().trim());
        }
        if (request.getGameName() != null) {
            tournament.setGameName(normalizeGameName(request.getGameName()));
        }
        if (request.getDescription() != null) {
            tournament.setDescription(request.getDescription());
        }
        if (request.getType() != null) {
            tournament.setType(request.getType());
        }
        if (request.getFormat() != null) {
            tournament.setFormat(request.getFormat());
        }
        if (request.getVisibility() != null) {
            tournament.setVisibility(request.getVisibility());
        }
        if (request.getParticipantsLimit() != null) {
            tournament.setParticipantsLimit(request.getParticipantsLimit());
        }
        if (request.getMinParticipants() != null) {
            tournament.setMinParticipants(request.getMinParticipants());
        }
        if (request.getEntryFeeCredits() != null) {
            tournament.setEntryFeeCredits(request.getEntryFeeCredits());
        }
        if (request.getFeePercentage() != null) {
            tournament.setFeePercentage(request.getFeePercentage());
        }
        if (request.getPrizeType() != null) {
            tournament.setPrizeType(request.getPrizeType());
        }
        if (request.getGroupsCount() != null) {
            tournament.setGroupsCount(request.getGroupsCount());
        }
        if (request.getTeamsPerGroup() != null) {
            tournament.setTeamsPerGroup(request.getTeamsPerGroup());
        }
        if (request.getAdvancePerGroup() != null) {
            tournament.setAdvancePerGroup(request.getAdvancePerGroup());
        }
        if (request.getBestOf() != null) {
            tournament.setBestOf(request.getBestOf());
        }
        if (request.getRules() != null) {
            tournament.setRules(request.getRules());
        }
        if (request.getTiebreakerRules() != null) {
            tournament.setTiebreakerRules(request.getTiebreakerRules());
        }
        if (request.getStartDate() != null) {
            tournament.setStartDate(request.getStartDate());
        }
        if (request.getRegistrationDeadline() != null) {
            tournament.setRegistrationDeadline(request.getRegistrationDeadline());
        }
        tournament.setRegistrationOpensAt(request.getRegistrationOpensAt());
        tournament.setExpectedEndDate(request.getExpectedEndDate());
        tournament.setGameImageUrl(normalizeUrl(request.getGameImageUrl()));
        tournament.setCoverImageUrl(normalizeUrl(request.getCoverImageUrl()));
        tournament.setLogoImageUrl(normalizeUrl(request.getLogoImageUrl()));
        tournament.setYoutubeUrl(normalizeUrl(request.getYoutubeUrl()));
        tournament.setTwitchUrl(normalizeUrl(request.getTwitchUrl()));

        if (request.getPresetId() != null) {
            Preset preset = presetRepository.findById(request.getPresetId())
                    .orElseThrow(() -> BusinessException.notFound("Preset não encontrado"));
            Long previousPresetId = tournament.getPreset() != null ? tournament.getPreset().getId() : null;
            tournament.setPreset(preset);
            if (!request.getPresetId().equals(previousPresetId)) {
                applyPresetSnapshot(tournament, preset);
            }
        }

        if (auth.isStaff() && request.getClientUserId() != null) {
            Client client = clientRepository.findById(request.getClientUserId())
                    .orElseThrow(() -> BusinessException.notFound("Cliente não encontrado"));
            tournament.setClient(client);
        }

        Tournament saved = tournamentRepository.save(tournament);
        auditStaff(auth, "UPDATE", saved, "Torneio atualizado: " + saved.getSlug());
        return saved;
    }

    private String normalizeGameName(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void applyPresetSnapshot(Tournament tournament, Preset preset) {
        if (preset == null) {
            return;
        }

        tournament.setGameName(normalizeGameName(preset.getGameName()));
        tournament.setFormat(resolveFormatFromPreset(preset));

        if (preset.getRulesTemplate() != null && !preset.getRulesTemplate().isBlank()) {
            tournament.setRules(preset.getRulesTemplate().trim());
        }

        String iconUrl = normalizeUrl(preset.getIconUrl());
        String gameImage = normalizeUrl(preset.getGameImageUrl());
        if (gameImage == null) {
            gameImage = iconUrl;
        }
        if (gameImage != null) {
            tournament.setGameImageUrl(gameImage);
        }

        if (tournament.getFormat() == TournamentFormat.SOLO
                && preset.getMinPlayersPerTeam() != null
                && preset.getMinPlayersPerTeam() > 0) {
            tournament.setMinParticipants(Math.max(2, preset.getMinPlayersPerTeam()));
        }
    }

    private TournamentFormat resolveFormatFromPreset(Preset preset) {
        return preset.getTeamSize() != null && preset.getTeamSize() > 1
                ? TournamentFormat.TEAM
                : TournamentFormat.SOLO;
    }

    private String normalizeUrl(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateEditable(Tournament tournament) {
        if (tournament.getStatus() == TournamentStatus.CANCELLED) {
            throw BusinessException.badRequest("Torneio cancelado não pode ser editado");
        }
    }

    public Page<Tournament> listMyCreated(AuthenticatedUser auth, Pageable pageable) {
        return tournamentRepository.findByOwnerTypeAndOwnerId(auth.getType(), auth.getId(), pageable);
    }

    public Page<Tournament> listMyManaged(AuthenticatedUser auth, Pageable pageable) {
        if (auth.isStaff()) {
            return tournamentRepository.findByOwnerTypeAndOwnerId(auth.getType(), auth.getId(), pageable);
        }

        Contact contact = identityService.requireContact(auth);
        if (tournamentAccessService.isPrimaryContact(contact)) {
            return tournamentRepository.findByClient_UserId(contact.getUserid(), pageable);
        }

        return tournamentRepository.findManagedByNonPrimaryContact(
                contact.getId(), contact.getId().longValue(), pageable);
    }

    public Page<Tournament> listMyJoined(AuthenticatedUser auth, Pageable pageable) {
        Contact contact = identityService.requireContact(auth);
        return tournamentRepository.findJoinedByContactId(contact.getId(), pageable);
    }

    @Transactional
    public Tournament updateStatus(String slug, TournamentStatus newStatus, AuthenticatedUser auth) {
        Tournament tournament = getBySlug(slug);
        tournamentAccessService.validateCanManage(tournament, auth);
        TournamentStatus previousStatus = tournament.getStatus();
        tournament.setStatus(newStatus);
        Tournament saved = tournamentRepository.save(tournament);
        auditStaff(auth, "UPDATE_STATUS", saved,
                "Status alterado de " + previousStatus + " para " + newStatus);
        return saved;
    }

    @Transactional
    public TournamentParticipant joinSolo(String slug, AuthenticatedUser auth, JoinTournamentRequest request) {
        Contact contact = identityService.requireContact(auth);
        Tournament tournament = getBySlug(slug);
        validateRegistrationOpen(tournament);

        if (participantRepository.existsByTournamentIdAndContactId(tournament.getId(), contact.getId())) {
            throw BusinessException.conflict("Já inscrito neste torneio");
        }

        if (tournament.getFormat() != TournamentFormat.SOLO) {
            throw BusinessException.badRequest("Torneio não é solo, use inscrição por time");
        }

        long currentCount = participantRepository.countByTournamentIdAndStatus(tournament.getId(), ParticipantStatus.APPROVED);
        if (currentCount >= tournament.getParticipantsLimit()) {
            throw BusinessException.badRequest("Torneio lotado");
        }

        if (tournamentAccessService.canManage(tournament, auth)) {
            throw BusinessException.forbidden("Organizador não pode participar como jogador");
        }

        if (tournament.getEntryFeeCredits().compareTo(BigDecimal.ZERO) > 0) {
            walletService.holdCredits(
                    contact.getUserid(), contact, tournament.getEntryFeeCredits(), "ENTRY_FEE", tournament.getId());
        }

        AvailabilityProfile profile = createAvailabilityProfile(contact, null, request);

        TournamentParticipant participant = TournamentParticipant.builder()
                .tournament(tournament)
                .contact(contact)
                .status(ParticipantStatus.APPROVED)
                .availabilityProfile(profile)
                .build();

        return participantRepository.save(participant);
    }

    @Transactional
    public TournamentParticipant joinTeam(String slug, AuthenticatedUser auth, JoinTournamentRequest request) {
        Contact contact = identityService.requireContact(auth);
        Tournament tournament = getBySlug(slug);
        validateRegistrationOpen(tournament);

        if (request.getTeamId() == null) {
            throw BusinessException.badRequest("ID do time é obrigatório");
        }

        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> BusinessException.notFound("Time não encontrado"));

        if (!team.getOwner().getId().equals(contact.getId())) {
            throw BusinessException.forbidden("Apenas o líder do time pode inscrever");
        }

        if (participantRepository.existsByTournamentIdAndTeamId(tournament.getId(), team.getId())) {
            throw BusinessException.conflict("Time já inscrito neste torneio");
        }

        Integer maxTournamentsPerTeam = teamSettingsService.getSettings().getMaxTournamentsPerTeam();
        if (maxTournamentsPerTeam != null) {
            long teamParticipations = participantRepository.countByTeamIdAndStatus(
                    team.getId(), ParticipantStatus.APPROVED);
            if (teamParticipations >= maxTournamentsPerTeam) {
                throw BusinessException.badRequest("Time atingiu o limite de torneios simultâneos");
            }
        }

        if (tournament.getFormat() != TournamentFormat.TEAM) {
            throw BusinessException.badRequest("Torneio não é por time");
        }

        if (tournamentAccessService.canManage(tournament, auth)) {
            throw BusinessException.forbidden("Organizador não pode participar como jogador");
        }

        long currentCount = participantRepository.countByTournamentIdAndStatus(tournament.getId(), ParticipantStatus.APPROVED);
        if (currentCount >= tournament.getParticipantsLimit()) {
            throw BusinessException.badRequest("Torneio lotado");
        }

        if (tournament.getEntryFeeCredits().compareTo(BigDecimal.ZERO) > 0) {
            walletService.holdCredits(
                    contact.getUserid(), contact, tournament.getEntryFeeCredits(), "ENTRY_FEE", tournament.getId());
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
    public void kickParticipant(String slug, Long participantId, AuthenticatedUser auth) {
        Tournament tournament = getBySlug(slug);
        tournamentAccessService.validateCanManage(tournament, auth);

        TournamentParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> BusinessException.notFound("Participante não encontrado"));

        participant.setStatus(ParticipantStatus.KICKED);
        participantRepository.save(participant);

        Integer refundContactId = participant.getContact() != null ? participant.getContact().getId() :
                (participant.getTeam() != null ? participant.getTeam().getOwner().getId() : null);
        if (refundContactId != null && tournament.getEntryFeeCredits().compareTo(BigDecimal.ZERO) > 0) {
            Contact refundContact = contactRepository.findById(refundContactId)
                    .orElseThrow(() -> BusinessException.notFound("Contato não encontrado"));
            walletService.releaseHold(refundContact.getUserid(), "ENTRY_FEE", tournament.getId());
        }

        auditStaff(auth, "KICK_PARTICIPANT", tournament,
                "Participante expulso: #" + participantId);
    }

    public List<TournamentParticipant> getParticipants(String slug) {
        Tournament tournament = getBySlug(slug);
        return participantRepository.findByTournamentId(tournament.getId());
    }

    @Transactional
    public void deleteTournament(String slug, AuthenticatedUser auth) {
        Tournament tournament = getBySlug(slug);
        tournamentAccessService.validateCanManage(tournament, auth);

        if (tournament.getStatus() == TournamentStatus.IN_PROGRESS) {
            throw BusinessException.badRequest("Não é possível excluir torneio em andamento");
        }

        tournament.setStatus(TournamentStatus.CANCELLED);
        tournamentRepository.save(tournament);
        auditStaff(auth, "CANCEL", tournament, "Torneio cancelado: " + slug);
    }

    @Transactional
    public TournamentManagerResponse grantManager(String slug, Integer contactId, AuthenticatedUser auth) {
        Tournament tournament = getBySlug(slug);
        tournamentAccessService.validateCanGrantManager(tournament, auth);

        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> BusinessException.notFound("Contato não encontrado"));

        if (!contact.getUserid().equals(tournament.getClient().getUserId())) {
            throw BusinessException.badRequest("Contato não pertence ao cliente do torneio");
        }

        if (tournamentAccessService.isPrimaryContact(contact)) {
            throw BusinessException.badRequest("Contato principal já possui acesso automático a todos os torneios do cliente");
        }

        if (managerPermissionRepository.existsByTournamentIdAndContactId(tournament.getId(), contactId)) {
            throw BusinessException.conflict("Contato já possui permissão para gerenciar este torneio");
        }

        TournamentManagerPermission permission = TournamentManagerPermission.builder()
                .tournament(tournament)
                .contact(contact)
                .build();

        TournamentManagerResponse response = TournamentManagerResponse.from(managerPermissionRepository.save(permission));
        auditStaff(auth, "GRANT_MANAGER", tournament, "Permissão concedida ao contato #" + contactId);
        return response;
    }

    @Transactional
    public void revokeManager(String slug, Integer contactId, AuthenticatedUser auth) {
        Tournament tournament = getBySlug(slug);
        tournamentAccessService.validateCanGrantManager(tournament, auth);

        if (!managerPermissionRepository.existsByTournamentIdAndContactId(tournament.getId(), contactId)) {
            throw BusinessException.notFound("Permissão não encontrada");
        }

        managerPermissionRepository.deleteByTournamentIdAndContactId(tournament.getId(), contactId);
        auditStaff(auth, "REVOKE_MANAGER", tournament, "Permissão revogada do contato #" + contactId);
    }

    public List<TournamentManagerResponse> listManagers(String slug, AuthenticatedUser auth) {
        Tournament tournament = getBySlug(slug);
        tournamentAccessService.validateCanManage(tournament, auth);

        return managerPermissionRepository.findByTournamentId(tournament.getId()).stream()
                .map(TournamentManagerResponse::from)
                .toList();
    }

    private void validateRegistrationOpen(Tournament tournament) {
        if (tournament.getStatus() != TournamentStatus.REGISTRATION_OPEN) {
            throw BusinessException.badRequest("Inscrições não estão abertas");
        }
    }

    public void validateOwnership(String slug, AuthenticatedUser auth) {
        Tournament tournament = getBySlug(slug);
        tournamentAccessService.validateCanManage(tournament, auth);
    }

    private AvailabilityProfile createAvailabilityProfile(Contact contact, Team team, JoinTournamentRequest request) {
        if (request.getAvailableWindows() == null || request.getAvailableWindows().isEmpty()) {
            return null;
        }

        AvailabilityProfile profile = AvailabilityProfile.builder()
                .contact(contact)
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

    private void auditStaff(AuthenticatedUser auth, String action, Tournament tournament, String message) {
        if (auth == null || !auth.isStaff() || tournament == null) {
            return;
        }
        auditService.recordStaffMessage(auth, action, "tournament", tournament.getId(), message);
    }
}
