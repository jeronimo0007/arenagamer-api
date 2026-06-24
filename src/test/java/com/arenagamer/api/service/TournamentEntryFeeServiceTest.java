package com.arenagamer.api.service;

import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.Tournament;
import com.arenagamer.api.entity.TournamentEntryFee;
import com.arenagamer.api.entity.TournamentParticipant;
import com.arenagamer.api.entity.enums.EntryFeeStatus;
import com.arenagamer.api.entity.enums.PrizeFunding;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.TournamentEntryFeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TournamentEntryFeeServiceTest {

    @Mock private TournamentEntryFeeRepository entryFeeRepository;
    @Mock private WalletService walletService;

    @InjectMocks
    private TournamentEntryFeeService entryFeeService;

    @Test
    void requireEntryFeeBalance_rejectsInsufficientBalance() {
        Tournament tournament = tournamentWithEntryFee(new BigDecimal("10"));
        doThrow(BusinessException.badRequest("Saldo insuficiente"))
                .when(walletService).requireAvailableBalance(42, new BigDecimal("10"));

        assertThatThrownBy(() -> entryFeeService.requireEntryFeeBalance(tournament, 42))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Saldo insuficiente");
    }

    @Test
    void chargeOnJoin_holdsCreditsPerParticipant() {
        Tournament tournament = tournamentWithEntryFee(new BigDecimal("5"));
        TournamentParticipant participant = TournamentParticipant.builder().id(99L).build();
        Contact payer = Contact.builder().id(1).userid(42).build();

        when(entryFeeRepository.findByParticipantId(99L)).thenReturn(Optional.empty());
        when(entryFeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        entryFeeService.chargeOnJoin(tournament, participant, payer);

        verify(walletService).holdCredits(42, payer, new BigDecimal("5"), "ENTRY_FEE", 99L);

        ArgumentCaptor<TournamentEntryFee> captor = ArgumentCaptor.forClass(TournamentEntryFee.class);
        verify(entryFeeRepository).save(captor.capture());
        assertThat(captor.getValue().getClientUserId()).isEqualTo(42);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("5");
        assertThat(captor.getValue().getStatus()).isEqualTo(EntryFeeStatus.HELD);
    }

    @Test
    void refund_releasesHoldAndMarksRefunded() {
        TournamentParticipant participant = TournamentParticipant.builder().id(99L).build();
        TournamentEntryFee fee = TournamentEntryFee.builder()
                .clientUserId(42)
                .amount(new BigDecimal("5"))
                .status(EntryFeeStatus.HELD)
                .build();

        when(entryFeeRepository.findByParticipantId(99L)).thenReturn(Optional.of(fee));
        when(entryFeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        entryFeeService.refund(participant);

        verify(walletService).releaseHold(42, "ENTRY_FEE", 99L);
        assertThat(fee.getStatus()).isEqualTo(EntryFeeStatus.REFUNDED);
        assertThat(fee.getRefundedAt()).isNotNull();
    }

    @Test
    void hasEntryFee_trueForFixedPrizeFundingWithPositiveEntryFee() {
        Tournament tournament = Tournament.builder()
                .prizeFunding(PrizeFunding.FIXED)
                .entryFeeCredits(new BigDecimal("10"))
                .build();

        assertThat(entryFeeService.hasEntryFee(tournament)).isTrue();
    }

    private static Tournament tournamentWithEntryFee(BigDecimal fee) {
        return Tournament.builder()
                .id(1L)
                .prizeFunding(PrizeFunding.ENTRY_FEES)
                .entryFeeCredits(fee)
                .build();
    }
}
