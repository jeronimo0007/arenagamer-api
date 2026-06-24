package com.arenagamer.api.repository;

import com.arenagamer.api.entity.TournamentParticipantPlayer;
import com.arenagamer.api.entity.enums.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TournamentParticipantPlayerRepository extends JpaRepository<TournamentParticipantPlayer, Long> {

    @Query("""
            SELECT tpp
            FROM TournamentParticipantPlayer tpp
            JOIN FETCH tpp.client
            WHERE tpp.participant.id = :participantId
            ORDER BY tpp.client.nickname, tpp.client.company
            """)
    List<TournamentParticipantPlayer> findByParticipantIdWithClient(@Param("participantId") Long participantId);

    @Query("""
            SELECT tpp
            FROM TournamentParticipantPlayer tpp
            JOIN FETCH tpp.client
            JOIN FETCH tpp.participant p
            WHERE p.tournament.id = :tournamentId
              AND (:status IS NULL OR p.status = :status)
            ORDER BY tpp.client.nickname, tpp.client.company
            """)
    List<TournamentParticipantPlayer> findByTournamentIdWithClient(
            @Param("tournamentId") Long tournamentId,
            @Param("status") ParticipantStatus status);

    /**
     * Jogadores já escalados (roster persistido) em outras equipes no mesmo torneio.
     * Membros do time que não foram escalados não geram conflito.
     */
    @Query("""
            SELECT DISTINCT c.userId, COALESCE(c.nickname, c.company), otherTeam.name
            FROM TournamentParticipantPlayer tpp
            JOIN tpp.participant tp
            JOIN tp.team otherTeam
            JOIN tpp.client c
            WHERE tp.tournament.id = :tournamentId
              AND tp.status = :participantStatus
              AND tp.team.id IS NOT NULL
              AND tp.team.id <> :excludeTeamId
              AND c.userId IN :clientUserIds
            """)
    List<Object[]> findRosterConflictsInTournament(
            @Param("tournamentId") Long tournamentId,
            @Param("excludeTeamId") Long excludeTeamId,
            @Param("clientUserIds") Collection<Integer> clientUserIds,
            @Param("participantStatus") ParticipantStatus participantStatus);

    @Query("""
            SELECT DISTINCT contact.userid, COALESCE(cl.nickname, cl.company)
            FROM TournamentParticipant tp
            JOIN tp.contact contact
            JOIN contact.client cl
            WHERE tp.tournament.id = :tournamentId
              AND tp.status = :participantStatus
              AND tp.contact IS NOT NULL
              AND contact.userid IN :clientUserIds
            """)
    List<Object[]> findSoloConflictsInTournament(
            @Param("tournamentId") Long tournamentId,
            @Param("clientUserIds") Collection<Integer> clientUserIds,
            @Param("participantStatus") ParticipantStatus participantStatus);
}
