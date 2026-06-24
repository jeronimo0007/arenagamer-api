package com.arenagamer.api.service;

import com.arenagamer.api.dto.response.TournamentEntryFeeItemResponse;
import com.arenagamer.api.dto.response.TournamentRevenueResponse;
import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.Tournament;
import com.arenagamer.api.entity.TournamentEntryFee;
import com.arenagamer.api.entity.TournamentParticipant;
import com.arenagamer.api.entity.enums.EntryFeeStatus;
import com.arenagamer.api.entity.enums.PrizeFunding;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.TournamentEntryFeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TournamentEntryFeeService {

    public static final String ENTRY_FEE_REFERENCE = "ENTRY_FEE";
    public static final String TOURNAMENT_CREATION_REFERENCE = "TOURNAMENT_CREATION";

    private final TournamentEntryFeeRepository entryFeeRepository;
    private final WalletService walletService;

    public boolean hasEntryFee(Tournament tournament) {
        return tournament.getEntryFeeCredits() != null
                && tournament.getEntryFeeCredits().compareTo(BigDecimal.ZERO) > 0;
    }

    public void requireEntryFeeBalance(Tournament tournament, Integer clientUserId) {
        if (!hasEntryFee(tournament)) {
            return;
        }
        walletService.requireAvailableBalance(clientUserId, tournament.getEntryFeeCredits());
    }

    @Transactional
    public void chargeOnJoin(Tournament tournament, TournamentParticipant participant, Contact payer) {
        if (!hasEntryFee(tournament)) {
            return;
        }

        if (entryFeeRepository.findByParticipantId(participant.getId()).isPresent()) {
            throw BusinessException.conflict("Taxa de inscrição já registrada para este participante");
        }

        BigDecimal amount = tournament.getEntryFeeCredits();
        walletService.holdCredits(payer.getUserid(), payer, amount, ENTRY_FEE_REFERENCE, participant.getId());

        entryFeeRepository.save(TournamentEntryFee.builder()
                .tournament(tournament)
                .participant(participant)
                .clientUserId(payer.getUserid())
                .amount(amount)
                .status(EntryFeeStatus.HELD)
                .build());
    }

    @Transactional
    public void refund(TournamentParticipant participant) {
        entryFeeRepository.findByParticipantId(participant.getId())
                .filter(fee -> fee.getStatus() == EntryFeeStatus.HELD)
                .ifPresent(fee -> {
                    walletService.releaseHold(fee.getClientUserId(), ENTRY_FEE_REFERENCE, participant.getId());
                    fee.setStatus(EntryFeeStatus.REFUNDED);
                    fee.setRefundedAt(LocalDateTime.now());
                    entryFeeRepository.save(fee);
                });
    }

    @Transactional
    public void captureAllForTournament(Tournament tournament) {
        if (!hasEntryFee(tournament)) {
            return;
        }

        List<TournamentEntryFee> heldFees = entryFeeRepository.findByTournamentIdAndStatus(
                tournament.getId(), EntryFeeStatus.HELD);

        Integer organizerClientUserId = tournament.getClient() != null
                ? tournament.getClient().getUserId()
                : null;
        boolean creditOrganizer = tournament.getPrizeFunding() == PrizeFunding.FIXED
                && organizerClientUserId != null;

        for (TournamentEntryFee fee : heldFees) {
            walletService.captureHold(
                    fee.getClientUserId(), ENTRY_FEE_REFERENCE, fee.getParticipant().getId());
            fee.setStatus(EntryFeeStatus.CAPTURED);
            entryFeeRepository.save(fee);

            if (creditOrganizer) {
                walletService.deposit(
                        organizerClientUserId,
                        null,
                        fee.getAmount(),
                        "Taxa de inscrição — torneio " + tournament.getName());
            }
        }
    }

    @Transactional
    public void refundAllHeldForTournament(Tournament tournament) {
        List<TournamentEntryFee> heldFees = entryFeeRepository.findByTournamentIdAndStatus(
                tournament.getId(), EntryFeeStatus.HELD);

        for (TournamentEntryFee fee : heldFees) {
            walletService.releaseHold(
                    fee.getClientUserId(), ENTRY_FEE_REFERENCE, fee.getParticipant().getId());
            fee.setStatus(EntryFeeStatus.REFUNDED);
            fee.setRefundedAt(LocalDateTime.now());
            entryFeeRepository.save(fee);
        }
    }

    @Transactional
    public void captureTournamentCreationPayment(Integer clientUserId, Long tournamentId) {
        walletService.captureHold(clientUserId, TOURNAMENT_CREATION_REFERENCE, null);
        walletService.linkHoldReference(clientUserId, TOURNAMENT_CREATION_REFERENCE, null, tournamentId);
    }

    @Transactional(readOnly = true)
    public TournamentRevenueResponse getRevenue(Tournament tournament, Integer clientUserIdFilter) {
        Long tournamentId = tournament.getId();

        BigDecimal collected = entryFeeRepository.sumAmountByTournamentIdAndStatus(
                tournamentId, EntryFeeStatus.HELD);
        BigDecimal captured = entryFeeRepository.sumAmountByTournamentIdAndStatus(
                tournamentId, EntryFeeStatus.CAPTURED);
        BigDecimal refunded = entryFeeRepository.sumAmountByTournamentIdAndStatus(
                tournamentId, EntryFeeStatus.REFUNDED);

        List<TournamentEntryFeeItemResponse> entries = entryFeeRepository
                .findByTournamentIdWithDetails(tournamentId)
                .stream()
                .filter(fee -> clientUserIdFilter == null
                        || clientUserIdFilter.equals(fee.getClientUserId()))
                .map(TournamentEntryFeeItemResponse::from)
                .toList();

        if (clientUserIdFilter != null) {
            collected = entries.stream()
                    .filter(e -> e.getStatus() == EntryFeeStatus.HELD)
                    .map(TournamentEntryFeeItemResponse::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return TournamentRevenueResponse.builder()
                .tournamentId(tournamentId)
                .tournamentSlug(tournament.getSlug())
                .tournamentName(tournament.getName())
                .entryFeeCredits(tournament.getEntryFeeCredits())
                .collectedCredits(collected)
                .capturedCredits(captured)
                .refundedCredits(refunded)
                .entries(entries)
                .build();
    }

    @Transactional(readOnly = true)
    public TournamentRevenueResponse getRevenue(Tournament tournament) {
        return getRevenue(tournament, null);
    }

    @Transactional(readOnly = true)
    public BigDecimal getCollectedCredits(Long tournamentId) {
        return entryFeeRepository.sumAmountByTournamentIdAndStatus(tournamentId, EntryFeeStatus.HELD);
    }
}
