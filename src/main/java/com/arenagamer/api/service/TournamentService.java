package com.arenagamer.api.service;

import com.arenagamer.api.dto.request.CreateTournamentRequest;
import com.arenagamer.api.dto.request.JoinTournamentRequest;
import com.arenagamer.api.dto.request.UpdateTournamentRequest;
import com.arenagamer.api.dto.response.TournamentManagerResponse;
import com.arenagamer.api.dto.response.TournamentParticipantsResponse;
import com.arenagamer.api.dto.response.TournamentResponse;
import com.arenagamer.api.dto.response.TournamentRevenueResponse;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private final AvailabilityService availabilityService;
    private final IdentityService identityService;
    private final TournamentAccessService tournamentAccessService;
    private final AuditService auditService;
    private final PlanEntitlementService planEntitlementService;
    private final TeamSettingsService teamSettingsService;
    private final TeamService teamService;
    private final TournamentParticipantPlayerRepository participantPlayerRepository;
    private final TournamentEntryFeeService tournamentEntryFeeService;
    private final TeamRosterService teamRosterService;

    private static final List<TournamentStatus> ACTIVE_TOURNAMENT_STATUSES = List.of(
            TournamentStatus.REGISTRATION_OPEN,
            TournamentStatus.REGISTRATION_CLOSED,
            TournamentStatus.IN_PROGRESS);

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
                        TournamentEntryFeeService.TOURNAMENT_CREATION_REFERENCE,
                        null);
            }
        }

        String slug = generateSlug(request.getName());

        Tournament tournament = Tournament.builder()
                .slug(slug)
                .name(request.getName())
                .description(request.getDescription())
                .ownerType(auth.getType())
                .ownerId(auth.getId())
                .type(request.getType())
                .format(request.getFormat())
                .visibility(request.getVisibility() != null ? request.getVisibility() : Visibility.PUBLIC)
                .status(TournamentStatus.DRAFT)
                .participantsLimit(request.getParticipantsLimit())
                .minParticipants(request.getMinParticipants())
                .minPlayersPerTeam(request.getFormat() == TournamentFormat.TEAM
                        ? request.getMinPlayersPerTeam()
                        : null)
                .maxPlayersPerTeam(request.getFormat() == TournamentFormat.TEAM
                        ? request.getMaxPlayersPerTeam()
                        : null)
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

        Preset preset = null;
        if (request.getPresetId() != null) {
            preset = presetRepository.findByIdAndActiveTrue(request.getPresetId())
                    .orElseThrow(() -> BusinessException.notFound("Preset não encontrado ou inativo"));
            tournament.setPreset(preset);
            applyPresetSnapshot(tournament, preset);
        } else {
            String gameName = normalizeGameName(request.getGameName());
            if (gameName == null) {
                throw BusinessException.badRequest("Informe o nome do jogo ou selecione um jogo predefinido");
            }
            tournament.setGameName(gameName);
        }

        resolveClientLink(auth, request, tournament);
        applyPrizeSettings(
                tournament,
                request.getPrizeType(),
                request.getPrizeFunding(),
                request.getPrizePool(),
                request.getEntryFeeCredits(),
                request.getFeePercentage());
        validateAndNormalizeFormatFields(tournament);

        Tournament saved = tournamentRepository.save(tournament);
        if (entitlement != null) {
            if (entitlement.creditCost().compareTo(BigDecimal.ZERO) > 0) {
                tournamentEntryFeeService.captureTournamentCreationPayment(contact.getUserid(), saved.getId());
            }
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
        TournamentResponse response = TournamentResponse.from(tournament, participantCount);
        if (tournament.getPrizeFunding() == PrizeFunding.ENTRY_FEES) {
            response.setCollectedEntryFeeCredits(
                    tournamentEntryFeeService.getCollectedCredits(tournament.getId()));
        }
        return response;
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
        validateFormatFieldsUnchangedWithParticipants(tournament, request);

        if (request.getName() != null && !request.getName().isBlank()) {
            tournament.setName(request.getName().trim());
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
        if (request.getMinPlayersPerTeam() != null) {
            tournament.setMinPlayersPerTeam(request.getMinPlayersPerTeam());
        }
        if (request.getMaxPlayersPerTeam() != null) {
            tournament.setMaxPlayersPerTeam(request.getMaxPlayersPerTeam());
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
        if (request.getPrizeFunding() != null) {
            tournament.setPrizeFunding(request.getPrizeFunding());
        }
        if (request.getPrizePool() != null) {
            tournament.setPrizePool(request.getPrizePool());
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
            Preset preset = presetRepository.findByIdAndActiveTrue(request.getPresetId())
                    .orElseThrow(() -> BusinessException.notFound("Preset não encontrado ou inativo"));
            tournament.setPreset(preset);
            applyPresetSnapshot(tournament, preset);
        } else if (request.getGameName() != null) {
            tournament.setPreset(null);
            String gameName = normalizeGameName(request.getGameName());
            if (gameName == null) {
                throw BusinessException.badRequest("Informe o nome do jogo ou selecione um jogo predefinido");
            }
            tournament.setGameName(gameName);
        }

        if (auth.isStaff() && request.getClientUserId() != null) {
            Client client = clientRepository.findById(request.getClientUserId())
                    .orElseThrow(() -> BusinessException.notFound("Cliente não encontrado"));
            tournament.setClient(client);
        }

        applyPrizeSettings(
                tournament,
                tournament.getPrizeType(),
                tournament.getPrizeFunding(),
                tournament.getPrizePool(),
                tournament.getEntryFeeCredits(),
                tournament.getFeePercentage());
        validateAndNormalizeFormatFields(tournament);

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

    private void applyPrizeSettings(
            Tournament tournament,
            PrizeType prizeType,
            PrizeFunding prizeFunding,
            BigDecimal prizePool,
            BigDecimal entryFeeCredits,
            BigDecimal feePercentage) {
        PrizeType resolvedType = prizeType != null ? prizeType : PrizeType.MANUAL;
        PrizeFunding resolvedFunding = prizeFunding != null ? prizeFunding : PrizeFunding.FIXED;

        BigDecimal entryFee = entryFeeCredits != null ? entryFeeCredits : BigDecimal.ZERO;
        BigDecimal fee = feePercentage != null ? feePercentage : BigDecimal.ZERO;
        BigDecimal pool = prizePool != null ? prizePool : BigDecimal.ZERO;

        if (resolvedFunding == PrizeFunding.ENTRY_FEES) {
            if (resolvedType != PrizeType.AUTOMATIC) {
                throw BusinessException.badRequest(
                        "Prêmio por arrecadação só é permitido com distribuição automática");
            }
            if (entryFee.compareTo(BigDecimal.ZERO) <= 0) {
                throw BusinessException.badRequest(
                        "Informe a taxa de inscrição para prêmio por arrecadação");
            }
            if (fee.compareTo(BigDecimal.ZERO) < 0 || fee.compareTo(new BigDecimal("100")) > 0) {
                throw BusinessException.badRequest("Taxa do organizador deve estar entre 0% e 100%");
            }
            if (fee.remainder(new BigDecimal("5")).compareTo(BigDecimal.ZERO) != 0) {
                throw BusinessException.badRequest(
                        "Taxa do organizador deve ser de 5% em 5% (0%, 5%, 10%...)");
            }
            pool = BigDecimal.ZERO;
        } else {
            fee = BigDecimal.ZERO;
            if (entryFee.compareTo(BigDecimal.ZERO) < 0) {
                throw BusinessException.badRequest("Taxa de inscrição não pode ser negativa");
            }
            if (pool.compareTo(BigDecimal.ZERO) < 0) {
                throw BusinessException.badRequest("Prêmio fixo não pode ser negativo");
            }
            if (resolvedType == PrizeType.AUTOMATIC && pool.compareTo(BigDecimal.ZERO) <= 0) {
                throw BusinessException.badRequest(
                        "Distribuição automática com prêmio fixo exige valor do prêmio maior que zero");
            }
        }

        tournament.setPrizeType(resolvedType);
        tournament.setPrizeFunding(resolvedFunding);
        tournament.setPrizePool(pool);
        tournament.setEntryFeeCredits(entryFee);
        tournament.setFeePercentage(fee);
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
            tournament.setMinParticipants(Math.max(4, preset.getMinPlayersPerTeam()));
        }

        if (tournament.getFormat() == TournamentFormat.TEAM
                && tournament.getMinPlayersPerTeam() == null
                && preset.getMinPlayersPerTeam() != null
                && preset.getMinPlayersPerTeam() > 0) {
            tournament.setMinPlayersPerTeam(preset.getMinPlayersPerTeam());
        }

        if (tournament.getFormat() == TournamentFormat.TEAM
                && tournament.getMaxPlayersPerTeam() == null
                && preset.getMaxPlayersPerTeam() != null
                && preset.getMaxPlayersPerTeam() > 0) {
            tournament.setMaxPlayersPerTeam(preset.getMaxPlayersPerTeam());
        }
    }

    private void validateAndNormalizeFormatFields(Tournament tournament) {
        if (tournament.getFormat() == TournamentFormat.SOLO) {
            tournament.setMinPlayersPerTeam(null);
            tournament.setMaxPlayersPerTeam(null);
            return;
        }

        if (tournament.getMinPlayersPerTeam() == null || tournament.getMinPlayersPerTeam() < 1) {
            throw BusinessException.badRequest(
                    "Torneio por equipe exige o tamanho mínimo da equipe (mín. jogadores por time)");
        }

        if (tournament.getMaxPlayersPerTeam() == null || tournament.getMaxPlayersPerTeam() < 1) {
            throw BusinessException.badRequest(
                    "Torneio por equipe exige o tamanho máximo da equipe (máx. jogadores por time)");
        }

        if (tournament.getMinPlayersPerTeam() > tournament.getMaxPlayersPerTeam()) {
            throw BusinessException.badRequest(
                    "O mínimo de jogadores por equipe não pode ser maior que o máximo");
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

    private void validateFormatFieldsUnchangedWithParticipants(
            Tournament tournament, UpdateTournamentRequest request) {
        if (countApprovedParticipants(tournament.getId()) <= 0) {
            return;
        }

        if (request.getType() != null && request.getType() != tournament.getType()) {
            throw BusinessException.badRequest(
                    "Formato e modo do torneio não podem ser alterados após haver inscrições");
        }

        if (request.getFormat() != null && request.getFormat() != tournament.getFormat()) {
            throw BusinessException.badRequest(
                    "Formato e modo do torneio não podem ser alterados após haver inscrições");
        }

        if (request.getMinPlayersPerTeam() != null
                && !request.getMinPlayersPerTeam().equals(tournament.getMinPlayersPerTeam())) {
            throw BusinessException.badRequest(
                    "Formato e modo do torneio não podem ser alterados após haver inscrições");
        }

        if (request.getMaxPlayersPerTeam() != null
                && !request.getMaxPlayersPerTeam().equals(tournament.getMaxPlayersPerTeam())) {
            throw BusinessException.badRequest(
                    "Formato e modo do torneio não podem ser alterados após haver inscrições");
        }

        if (request.getGroupsCount() != null
                && !request.getGroupsCount().equals(tournament.getGroupsCount())) {
            throw BusinessException.badRequest(
                    "Formato e modo do torneio não podem ser alterados após haver inscrições");
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

        if (newStatus == TournamentStatus.IN_PROGRESS && previousStatus != TournamentStatus.IN_PROGRESS) {
            tournamentEntryFeeService.captureAllForTournament(saved);
            teamRosterService.forfeitOpenVacanciesOnTournamentStart(saved.getId());
        }
        if (newStatus == TournamentStatus.CANCELLED && previousStatus != TournamentStatus.CANCELLED) {
            tournamentEntryFeeService.refundAllHeldForTournament(saved);
        }

        auditStaff(auth, "UPDATE_STATUS", saved,
                "Status alterado de " + previousStatus + " para " + newStatus);
        return saved;
    }

    @Transactional
    public TournamentParticipant joinSolo(String slug, AuthenticatedUser auth, JoinTournamentRequest request) {
        Contact contact = identityService.requireContact(auth);
        Tournament tournament = getBySlug(slug);
        validateRegistrationOpen(tournament);

        if (participantRepository.existsByTournamentIdAndContactIdAndStatus(
                tournament.getId(), contact.getId(), ParticipantStatus.APPROVED)) {
            throw BusinessException.conflict("Já inscrito neste torneio");
        }

        if (tournament.getFormat() != TournamentFormat.SOLO) {
            throw BusinessException.badRequest("Torneio é por equipe. Inscreva um time.");
        }

        long currentCount = participantRepository.countByTournamentIdAndStatus(tournament.getId(), ParticipantStatus.APPROVED);
        if (currentCount >= tournament.getParticipantsLimit()) {
            throw BusinessException.badRequest("Torneio lotado");
        }

        if (tournamentAccessService.canManage(tournament, auth)) {
            throw BusinessException.forbidden("Organizador não pode participar como jogador");
        }

        validateClientParticipationLimit(contact.getUserid());
        tournamentEntryFeeService.requireEntryFeeBalance(tournament, contact.getUserid());

        AvailabilityProfile profile = resolveParticipantAvailability(contact, null, request);

        TournamentParticipant participant = TournamentParticipant.builder()
                .tournament(tournament)
                .contact(contact)
                .status(ParticipantStatus.APPROVED)
                .availabilityProfile(profile)
                .build();

        participant = participantRepository.save(participant);
        tournamentEntryFeeService.chargeOnJoin(tournament, participant, contact);
        return participant;
    }

    @Transactional
    public TournamentParticipant joinTeam(String slug, AuthenticatedUser auth, JoinTournamentRequest request) {
        Contact contact = identityService.requireContact(auth);
        Tournament tournament = getBySlug(slug);
        validateRegistrationOpen(tournament);

        if (request.getTeamId() == null) {
            throw BusinessException.badRequest("ID do time é obrigatório");
        }

        Team team = teamRepository.findByIdWithDetails(request.getTeamId())
                .orElseThrow(() -> BusinessException.notFound("Time não encontrado"));

        if (!teamService.canRegisterTeamInTournament(contact, team)) {
            throw BusinessException.forbidden(
                    "Apenas o contato primário do cliente dono ou o capitão do time pode inscrever a equipe");
        }

        if (participantRepository.existsByTournamentIdAndTeamIdAndStatus(
                tournament.getId(), team.getId(), ParticipantStatus.APPROVED)) {
            throw BusinessException.conflict("Time já inscrito neste torneio");
        }

        validateTeamParticipationLimit(team.getId());

        if (tournament.getFormat() != TournamentFormat.TEAM) {
            throw BusinessException.badRequest("Torneio não é por equipe. Inscreva-se individualmente.");
        }

        List<Client> roster = resolveTeamRoster(team, tournament, request);
        validatePlayersNotAlreadyInTournament(tournament.getId(), team.getId(), roster);
        for (Client player : roster) {
            validateClientParticipationLimit(player.getUserId());
        }

        if (tournamentAccessService.canManage(tournament, auth)) {
            throw BusinessException.forbidden("Organizador não pode participar como jogador");
        }

        long currentCount = participantRepository.countByTournamentIdAndStatus(tournament.getId(), ParticipantStatus.APPROVED);
        if (currentCount >= tournament.getParticipantsLimit()) {
            throw BusinessException.badRequest("Torneio lotado");
        }

        tournamentEntryFeeService.requireEntryFeeBalance(tournament, contact.getUserid());

        AvailabilityProfile profile = resolveParticipantAvailability(null, team, request);

        TournamentParticipant participant = TournamentParticipant.builder()
                .tournament(tournament)
                .team(team)
                .status(ParticipantStatus.APPROVED)
                .availabilityProfile(profile)
                .build();

        participant = participantRepository.save(participant);
        tournamentEntryFeeService.chargeOnJoin(tournament, participant, contact);
        saveParticipantRoster(participant, roster);
        return participant;
    }

    @Transactional
    public void kickParticipant(String slug, Long participantId, AuthenticatedUser auth) {
        Tournament tournament = getBySlug(slug);
        tournamentAccessService.validateCanManage(tournament, auth);

        TournamentParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> BusinessException.notFound("Participante não encontrado"));

        if (!participant.getTournament().getId().equals(tournament.getId())) {
            throw BusinessException.notFound("Participante não encontrado neste torneio");
        }

        participant.setStatus(ParticipantStatus.KICKED);
        participantRepository.save(participant);
        tournamentEntryFeeService.refund(participant);

        auditStaff(auth, "KICK_PARTICIPANT", tournament,
                "Participante expulso: #" + participantId);
    }

    @Transactional
    public void withdrawFromTournament(String slug, AuthenticatedUser auth, Long teamId) {
        Contact contact = identityService.requireContact(auth);
        Tournament tournament = getBySlug(slug);
        validateWithdrawalAllowed(tournament);

        TournamentParticipant participant = resolveWithdrawableParticipant(tournament, contact, teamId);
        if (participant.getStatus() != ParticipantStatus.APPROVED) {
            throw BusinessException.badRequest("Inscrição não está ativa");
        }

        participant.setStatus(ParticipantStatus.WITHDRAWN);
        participantRepository.save(participant);
        tournamentEntryFeeService.refund(participant);
    }

    /**
     * Remove inscrições ativas do time antes de excluí-lo.
     * Torneio não iniciado (até 1 dia antes do startDate): desinscreve com reembolso da taxa, se houver.
     * Torneio já iniciado: desinscreve sem reembolso (derrota automática).
     */
    @Transactional
    public void releaseTeamForDeletion(Long teamId) {
        List<TournamentParticipant> participations = participantRepository.findByTeamIdAndStatusWithTournament(
                teamId, ParticipantStatus.APPROVED);

        for (TournamentParticipant participant : participations) {
            Tournament tournament = participant.getTournament();
            participant.setStatus(ParticipantStatus.WITHDRAWN);

            if (!hasTournamentStarted(tournament) && isRefundEligibleOnWithdrawal(tournament)) {
                tournamentEntryFeeService.refund(participant);
            }

            participant.setTeam(null);
            participantRepository.save(participant);
        }
    }

    private boolean hasTournamentStarted(Tournament tournament) {
        return tournament.getStatus() == TournamentStatus.IN_PROGRESS
                || tournament.getStatus() == TournamentStatus.COMPLETED;
    }

    private boolean isRefundEligibleOnWithdrawal(Tournament tournament) {
        if (tournament.getStatus() == TournamentStatus.CANCELLED
                || tournament.getStatus() == TournamentStatus.COMPLETED) {
            return false;
        }
        if (tournament.getStartDate() == null) {
            return true;
        }
        return LocalDate.now().isBefore(tournament.getStartDate().toLocalDate());
    }

    private TournamentParticipant resolveWithdrawableParticipant(
            Tournament tournament, Contact contact, Long teamId) {
        if (tournament.getFormat() == TournamentFormat.SOLO) {
            if (teamId != null) {
                throw BusinessException.badRequest("Torneio solo — não informe teamId");
            }
            return participantRepository.findByTournamentIdAndContactId(tournament.getId(), contact.getId())
                    .filter(p -> p.getStatus() == ParticipantStatus.APPROVED)
                    .or(() -> participantRepository.findSoloByTournamentIdAndClientUserIdAndStatus(
                            tournament.getId(), contact.getUserid(), ParticipantStatus.APPROVED))
                    .orElseThrow(() -> BusinessException.notFound("Você não está inscrito neste torneio"));
        }

        if (teamId == null) {
            throw BusinessException.badRequest("Informe o teamId para desinscrever o time");
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> BusinessException.notFound("Time não encontrado"));

        if (!teamService.canRegisterTeamInTournament(contact, team)) {
            throw BusinessException.forbidden(
                    "Apenas o contato primário do cliente dono ou o capitão pode desinscrever o time");
        }

        return participantRepository.findByTournamentIdAndTeamId(tournament.getId(), teamId)
                .filter(p -> p.getStatus() == ParticipantStatus.APPROVED)
                .orElseThrow(() -> BusinessException.notFound("Este time não está inscrito neste torneio"));
    }

    private void validateWithdrawalAllowed(Tournament tournament) {
        if (tournament.getStatus() == TournamentStatus.IN_PROGRESS
                || tournament.getStatus() == TournamentStatus.COMPLETED
                || tournament.getStatus() == TournamentStatus.CANCELLED) {
            throw BusinessException.badRequest("Não é possível desinscrever-se de um torneio já iniciado ou encerrado");
        }

        if (tournament.getStartDate() == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate startDay = tournament.getStartDate().toLocalDate();
        if (!today.isBefore(startDay)) {
            throw BusinessException.badRequest(
                    "Desinscrição permitida apenas até o dia anterior ao início do torneio");
        }
    }

    @Transactional(readOnly = true)
    public TournamentRevenueResponse getEntryFeeRevenue(String slug, AuthenticatedUser auth, Integer clientUserId) {
        Tournament tournament = getBySlug(slug);
        tournamentAccessService.validateCanManage(tournament, auth);
        return tournamentEntryFeeService.getRevenue(tournament, clientUserId);
    }

    public List<TournamentParticipant> getParticipants(String slug) {
        Tournament tournament = getBySlug(slug);
        return participantRepository.findByTournamentId(tournament.getId());
    }

    @Transactional(readOnly = true)
    public TournamentParticipantsResponse listParticipants(
            String slug, AuthenticatedUser auth, ParticipantStatus status) {
        Tournament tournament = getBySlug(slug);
        validateCanViewParticipants(tournament, auth);

        ParticipantStatus effectiveStatus = resolveParticipantListStatus(tournament, auth, status);
        List<TournamentParticipant> participants = participantRepository.findByTournamentIdWithDetails(
                tournament.getId(), effectiveStatus);

        List<TournamentParticipantPlayer> rosterPlayers = tournament.getFormat() == TournamentFormat.TEAM
                ? participantPlayerRepository.findByTournamentIdWithClient(tournament.getId(), effectiveStatus)
                : List.of();

        return TournamentParticipantsResponse.from(tournament, participants, rosterPlayers);
    }

    private void validateCanViewParticipants(Tournament tournament, AuthenticatedUser auth) {
        if (tournament.getVisibility() == Visibility.PUBLIC
                || tournament.getVisibility() == Visibility.PROTECTED) {
            return;
        }
        if (auth != null && tournamentAccessService.canManage(tournament, auth)) {
            return;
        }
        throw BusinessException.forbidden("Lista de participantes restrita a torneios públicos ou organizadores");
    }

    private ParticipantStatus resolveParticipantListStatus(
            Tournament tournament, AuthenticatedUser auth, ParticipantStatus requestedStatus) {
        if (requestedStatus != null) {
            if (auth != null && tournamentAccessService.canManage(tournament, auth)) {
                return requestedStatus;
            }
            if (requestedStatus == ParticipantStatus.APPROVED) {
                return ParticipantStatus.APPROVED;
            }
            throw BusinessException.forbidden("Somente organizadores podem filtrar por outros status");
        }
        return ParticipantStatus.APPROVED;
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
        tournamentEntryFeeService.refundAllHeldForTournament(tournament);
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

    /**
     * Solo: horários do jogador (cliente). Time: horários do time para todos os jogadores escalados.
     */
    private AvailabilityProfile resolveParticipantAvailability(
            Contact contact, Team team, JoinTournamentRequest request) {
        if (team != null) {
            AvailabilityProfile teamProfile = availabilityService.getTeamProfile(team.getId());
            AvailabilityProfile snapshot = availabilityService.snapshotForTournament(teamProfile);
            if (snapshot != null) {
                return snapshot;
            }
        } else if (contact != null && contact.getUserid() != null) {
            AvailabilityProfile clientProfile = availabilityService.getClientProfile(contact.getUserid());
            AvailabilityProfile snapshot = availabilityService.snapshotForTournament(clientProfile);
            if (snapshot != null) {
                return snapshot;
            }
        }
        return createLegacyAvailabilityFromRequest(request);
    }

    private AvailabilityProfile createLegacyAvailabilityFromRequest(JoinTournamentRequest request) {
        if (request.getAvailableWindows() == null || request.getAvailableWindows().isEmpty()) {
            return null;
        }

        AvailabilityProfile profile = AvailabilityProfile.builder()
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

    private void validateTeamParticipationLimit(Long teamId) {
        Integer maxTournamentsPerTeam = teamSettingsService.getSettings().getMaxTournamentsPerTeam();
        if (maxTournamentsPerTeam == null) {
            return;
        }
        long activeParticipations = participantRepository.findActiveByTeamId(
                teamId, ParticipantStatus.APPROVED, ACTIVE_TOURNAMENT_STATUSES).size();
        if (activeParticipations >= maxTournamentsPerTeam) {
            throw BusinessException.badRequest("Esta equipe atingiu o limite de campeonatos simultâneos");
        }
    }

    private void validateClientParticipationLimit(Integer clientUserId) {
        if (clientUserId == null) {
            return;
        }
        Integer maxTournamentsPerClient = teamSettingsService.getSettings().getMaxTournamentsPerClient();
        if (maxTournamentsPerClient == null) {
            return;
        }
        long activeParticipations = participantRepository.countActiveParticipationsByClientUserId(
                clientUserId, ParticipantStatus.APPROVED, ACTIVE_TOURNAMENT_STATUSES);
        if (activeParticipations >= maxTournamentsPerClient) {
            throw BusinessException.badRequest(
                    "O jogador atingiu o limite de campeonatos simultâneos");
        }
    }

    private List<Client> resolveTeamRoster(Team team, Tournament tournament, JoinTournamentRequest request) {
        Set<Integer> memberUserIds = collectTeamMemberUserIds(team);
        if (memberUserIds.isEmpty()) {
            throw BusinessException.badRequest("Time sem membros não pode se inscrever");
        }

        Integer maxPlayers = tournament.getMaxPlayersPerTeam();
        Integer minPlayers = tournament.getMinPlayersPerTeam();
        boolean rosterRequested = hasRosterRequest(request);
        int memberCount = memberUserIds.size();

        if (maxPlayers != null && memberCount > maxPlayers && !rosterRequested) {
            throw BusinessException.badRequest(
                    "O time possui " + memberCount + " jogador(es). Escale no máximo "
                            + maxPlayers + " com playerNicknames ou playerClientUserIds");
        }

        List<Client> roster = rosterRequested
                ? resolveRosterFromRequest(request, memberUserIds)
                : loadTeamMemberClients(memberUserIds);

        if (minPlayers != null && roster.size() < minPlayers) {
            throw BusinessException.badRequest(
                    "A escalação possui " + roster.size() + " jogador(es), mas este torneio exige no mínimo "
                            + minPlayers + " por equipe");
        }
        if (maxPlayers != null && roster.size() > maxPlayers) {
            throw BusinessException.badRequest(
                    "A escalação possui " + roster.size() + " jogador(es), mas o limite deste torneio é "
                            + maxPlayers + " por equipe");
        }
        return roster;
    }

    private Set<Integer> collectTeamMemberUserIds(Team team) {
        Set<Integer> memberUserIds = new HashSet<>();
        if (team.getClient() != null && team.getClient().getUserId() != null) {
            memberUserIds.add(team.getClient().getUserId());
        }
        if (team.getMembers() != null) {
            team.getMembers().stream()
                    .map(TeamMember::getClient)
                    .filter(Objects::nonNull)
                    .map(Client::getUserId)
                    .filter(Objects::nonNull)
                    .forEach(memberUserIds::add);
        }
        return memberUserIds;
    }

    private boolean hasRosterRequest(JoinTournamentRequest request) {
        return (request.getPlayerClientUserIds() != null && !request.getPlayerClientUserIds().isEmpty())
                || (request.getPlayerNicknames() != null && !request.getPlayerNicknames().isEmpty());
    }

    private List<Client> resolveRosterFromRequest(JoinTournamentRequest request, Set<Integer> memberUserIds) {
        if (request.getPlayerClientUserIds() != null && !request.getPlayerClientUserIds().isEmpty()
                && request.getPlayerNicknames() != null && !request.getPlayerNicknames().isEmpty()) {
            throw BusinessException.badRequest("Informe playerClientUserIds ou playerNicknames, não ambos");
        }

        if (request.getPlayerClientUserIds() != null && !request.getPlayerClientUserIds().isEmpty()) {
            Set<Integer> uniqueIds = new HashSet<>(request.getPlayerClientUserIds());
            if (uniqueIds.size() != request.getPlayerClientUserIds().size()) {
                throw BusinessException.badRequest("Há jogadores duplicados na escalação");
            }
            List<Client> roster = new ArrayList<>();
            for (Integer clientUserId : request.getPlayerClientUserIds()) {
                if (!memberUserIds.contains(clientUserId)) {
                    throw BusinessException.badRequest("Jogador #" + clientUserId + " não pertence a este time");
                }
                Client client = clientRepository.findById(clientUserId)
                        .orElseThrow(() -> BusinessException.badRequest("Jogador não encontrado: " + clientUserId));
                roster.add(client);
            }
            return roster;
        }

        List<String> nicknames = request.getPlayerNicknames();
        if (nicknames == null || nicknames.isEmpty()) {
            throw BusinessException.badRequest("Escalação de jogadores é obrigatória para este time");
        }
        Set<String> uniqueNicknames = new HashSet<>();
        List<Client> roster = new ArrayList<>();
        for (String nickname : nicknames) {
            if (nickname == null || nickname.isBlank()) {
                throw BusinessException.badRequest("Nickname inválido na escalação");
            }
            String normalized = nickname.trim();
            if (!uniqueNicknames.add(normalized.toLowerCase())) {
                throw BusinessException.badRequest("Há nicknames duplicados na escalação");
            }
            Client client = clientRepository.findByNicknameIgnoreCase(normalized)
                    .orElseThrow(() -> BusinessException.badRequest("Jogador não encontrado: " + normalized));
            if (!memberUserIds.contains(client.getUserId())) {
                throw BusinessException.badRequest("Jogador " + normalized + " não pertence a este time");
            }
            roster.add(client);
        }
        return roster;
    }

    private List<Client> loadTeamMemberClients(Set<Integer> memberUserIds) {
        return memberUserIds.stream()
                .map(id -> clientRepository.findById(id)
                        .orElseThrow(() -> BusinessException.badRequest("Jogador não encontrado: " + id)))
                .toList();
    }

    private void validatePlayersNotAlreadyInTournament(Long tournamentId, Long teamId, List<Client> roster) {
        List<Integer> clientUserIds = roster.stream()
                .map(Client::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (clientUserIds.isEmpty()) {
            return;
        }
        Map<Integer, ConflictInfo> conflicts = new LinkedHashMap<>();

        participantPlayerRepository.findRosterConflictsInTournament(
                        tournamentId, teamId, clientUserIds, ParticipantStatus.APPROVED)
                .forEach(row -> conflicts.putIfAbsent(
                        ((Number) row[0]).intValue(),
                        new ConflictInfo(displayName(row[1]), (String) row[2])));

        participantPlayerRepository.findSoloConflictsInTournament(
                        tournamentId, clientUserIds, ParticipantStatus.APPROVED)
                .forEach(row -> conflicts.putIfAbsent(
                        ((Number) row[0]).intValue(),
                        new ConflictInfo(displayName(row[1]), null)));

        if (conflicts.isEmpty()) {
            return;
        }

        String details = conflicts.values().stream()
                .map(conflict -> conflict.teamName() != null
                        ? conflict.playerName() + " (já escalado no time " + conflict.teamName() + ")"
                        : conflict.playerName() + " (já inscrito individualmente neste torneio)")
                .collect(Collectors.joining("; "));

        throw BusinessException.conflict(
                "Um ou mais jogadores escalados já participam deste torneio por outra equipe: " + details);
    }

    private void saveParticipantRoster(TournamentParticipant participant, List<Client> roster) {
        for (Client client : roster) {
            participantPlayerRepository.save(TournamentParticipantPlayer.builder()
                    .participant(participant)
                    .tournament(participant.getTournament())
                    .client(client)
                    .build());
        }
    }

    private String displayName(Object value) {
        return value != null ? value.toString() : "Jogador";
    }

    private record ConflictInfo(String playerName, String teamName) {}
}
