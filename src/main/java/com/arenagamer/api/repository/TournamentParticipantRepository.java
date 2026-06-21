package com.arenagamer.api.repository;

import com.arenagamer.api.entity.TournamentParticipant;
import com.arenagamer.api.entity.enums.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TournamentParticipantRepository extends JpaRepository<TournamentParticipant, Long> {
    List<TournamentParticipant> findByTournamentId(Long tournamentId);
    List<TournamentParticipant> findByTournamentIdAndStatus(Long tournamentId, ParticipantStatus status);
    Optional<TournamentParticipant> findByTournamentIdAndContactId(Long tournamentId, Integer contactId);
    Optional<TournamentParticipant> findByTournamentIdAndTeamId(Long tournamentId, Long teamId);
    long countByTournamentIdAndStatus(Long tournamentId, ParticipantStatus status);

    @Query("""
            SELECT tp.tournament.id, COUNT(tp)
            FROM TournamentParticipant tp
            WHERE tp.tournament.id IN :tournamentIds
              AND tp.status = :status
            GROUP BY tp.tournament.id
            """)
    List<Object[]> countByTournamentIdsAndStatus(@Param("tournamentIds") Collection<Long> tournamentIds,
                                                 @Param("status") ParticipantStatus status);

    boolean existsByTournamentIdAndContactId(Long tournamentId, Integer contactId);
    boolean existsByTournamentIdAndTeamId(Long tournamentId, Long teamId);
    long countByTeamIdAndStatus(Long teamId, ParticipantStatus status);
}
