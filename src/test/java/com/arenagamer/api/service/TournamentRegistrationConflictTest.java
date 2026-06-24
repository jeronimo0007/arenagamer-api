package com.arenagamer.api.service;

import com.arenagamer.api.entity.Client;
import com.arenagamer.api.entity.enums.ParticipantStatus;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.TournamentParticipantPlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentRegistrationConflictTest {

    @Mock private TournamentParticipantPlayerRepository participantPlayerRepository;

    @InjectMocks
    private TournamentService tournamentService;

    @Test
    void validatePlayersNotAlreadyInTournament_rejectsOverlappingRoster() throws Exception {
        when(participantPlayerRepository.findRosterConflictsInTournament(
                eq(10L), eq(1L), eq(List.of(2, 3)), eq(ParticipantStatus.APPROVED)))
                .thenReturn(List.<Object[]>of(new Object[] {2, "Player Two", "Team Gamma"}));
        when(participantPlayerRepository.findSoloConflictsInTournament(
                eq(10L), eq(List.of(2, 3)), eq(ParticipantStatus.APPROVED)))
                .thenReturn(List.of());

        Method method = TournamentService.class.getDeclaredMethod(
                "validatePlayersNotAlreadyInTournament", Long.class, Long.class, List.class);
        method.setAccessible(true);

        List<Client> roster = List.of(
                Client.builder().userId(2).nickname("Player Two").build(),
                Client.builder().userId(3).nickname("Player Three").build());

        assertThatThrownBy(() -> method.invoke(tournamentService, 10L, 1L, roster))
                .hasCauseInstanceOf(BusinessException.class)
                .cause()
                .hasMessageContaining("já escalado")
                .hasMessageContaining("Team Gamma");
    }
}
