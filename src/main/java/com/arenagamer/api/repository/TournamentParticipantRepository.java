package com.arenagamer.api.repository;

import com.arenagamer.api.entity.TournamentParticipant;
import com.arenagamer.api.entity.enums.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TournamentParticipantRepository extends JpaRepository<TournamentParticipant, Long> {
    List<TournamentParticipant> findByTournamentId(Long tournamentId);
    List<TournamentParticipant> findByTournamentIdAndStatus(Long tournamentId, ParticipantStatus status);
    Optional<TournamentParticipant> findByTournamentIdAndUserId(Long tournamentId, Long userId);
    Optional<TournamentParticipant> findByTournamentIdAndTeamId(Long tournamentId, Long teamId);
    long countByTournamentIdAndStatus(Long tournamentId, ParticipantStatus status);
    boolean existsByTournamentIdAndUserId(Long tournamentId, Long userId);
    boolean existsByTournamentIdAndTeamId(Long tournamentId, Long teamId);
}
