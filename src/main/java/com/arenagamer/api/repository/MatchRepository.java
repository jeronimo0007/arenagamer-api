package com.arenagamer.api.repository;

import com.arenagamer.api.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByRoundId(Long roundId);

    @Query("SELECT m FROM Match m WHERE m.round.tournament.id = :tournamentId ORDER BY m.round.roundNumber, m.matchNumber")
    List<Match> findByTournamentId(Long tournamentId);
}
