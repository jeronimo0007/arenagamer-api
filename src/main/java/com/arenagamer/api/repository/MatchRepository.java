package com.arenagamer.api.repository;

import com.arenagamer.api.entity.enums.MatchStatus;
import com.arenagamer.api.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByRoundId(Long roundId);

    @Query("""
            SELECT m FROM Match m
            LEFT JOIN FETCH m.round r
            LEFT JOIN FETCH m.homeParticipant hp
            LEFT JOIN FETCH hp.contact hpc
            LEFT JOIN FETCH hpc.client
            LEFT JOIN FETCH hp.team
            LEFT JOIN FETCH m.awayParticipant ap
            LEFT JOIN FETCH ap.contact apc
            LEFT JOIN FETCH apc.client
            LEFT JOIN FETCH ap.team
            LEFT JOIN FETCH m.winnerParticipant
            WHERE m.id = :matchId
            """)
    Optional<Match> findByIdWithParticipants(@Param("matchId") Long matchId);

    @Query("SELECT m FROM Match m WHERE m.round.tournament.id = :tournamentId ORDER BY m.round.roundNumber, m.matchNumber")
    List<Match> findByTournamentId(Long tournamentId);

    @Query("""
            SELECT DISTINCT m FROM Match m
            LEFT JOIN FETCH m.round r
            LEFT JOIN FETCH m.homeParticipant hp
            LEFT JOIN FETCH hp.contact hpc
            LEFT JOIN FETCH hpc.client
            LEFT JOIN FETCH hp.team
            LEFT JOIN FETCH m.awayParticipant ap
            LEFT JOIN FETCH ap.contact apc
            LEFT JOIN FETCH apc.client
            LEFT JOIN FETCH ap.team
            LEFT JOIN FETCH m.winnerParticipant
            WHERE r.tournament.id = :tournamentId
            ORDER BY r.roundNumber, m.matchNumber
            """)
    List<Match> findByTournamentIdWithParticipants(@Param("tournamentId") Long tournamentId);

    @Query("""
            SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
            FROM Match m
            WHERE (m.homeParticipant.id = :participantId OR m.awayParticipant.id = :participantId)
              AND m.status IN :statuses
            """)
    boolean existsByParticipantIdAndStatusIn(
            @Param("participantId") Long participantId,
            @Param("statuses") Collection<MatchStatus> statuses);
}
