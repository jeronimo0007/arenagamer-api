package com.arenagamer.api.repository;

import com.arenagamer.api.entity.TournamentParticipant;
import com.arenagamer.api.entity.enums.ParticipantStatus;
import com.arenagamer.api.entity.enums.TournamentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TournamentParticipantRepository extends JpaRepository<TournamentParticipant, Long> {
    List<TournamentParticipant> findByTournamentId(Long tournamentId);
    List<TournamentParticipant> findByTournamentIdAndStatus(Long tournamentId, ParticipantStatus status);

    @Query("""
            SELECT DISTINCT tp
            FROM TournamentParticipant tp
            LEFT JOIN FETCH tp.contact c
            LEFT JOIN FETCH c.client
            LEFT JOIN FETCH tp.team t
            WHERE tp.tournament.id = :tournamentId
              AND (:status IS NULL OR tp.status = :status)
            ORDER BY tp.registeredAt ASC
            """)
    List<TournamentParticipant> findByTournamentIdWithDetails(
            @Param("tournamentId") Long tournamentId,
            @Param("status") ParticipantStatus status);

    Optional<TournamentParticipant> findByTournamentIdAndContactId(Long tournamentId, Integer contactId);
    Optional<TournamentParticipant> findByTournamentIdAndTeamId(Long tournamentId, Long teamId);

    @Query("""
            SELECT tp
            FROM TournamentParticipant tp
            JOIN tp.contact c
            WHERE tp.tournament.id = :tournamentId
              AND c.userid = :clientUserId
              AND tp.contact IS NOT NULL
              AND tp.status = :status
            """)
    Optional<TournamentParticipant> findSoloByTournamentIdAndClientUserIdAndStatus(
            @Param("tournamentId") Long tournamentId,
            @Param("clientUserId") Integer clientUserId,
            @Param("status") ParticipantStatus status);

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
    boolean existsByTournamentIdAndContactIdAndStatus(
            Long tournamentId, Integer contactId, ParticipantStatus status);
    boolean existsByTournamentIdAndTeamIdAndStatus(
            Long tournamentId, Long teamId, ParticipantStatus status);
    boolean existsByTeam_Id(Long teamId);
    long countByTeamIdAndStatus(Long teamId, ParticipantStatus status);

    @Query("""
            SELECT tp
            FROM TournamentParticipant tp
            JOIN FETCH tp.tournament t
            LEFT JOIN FETCH t.preset
            WHERE tp.team.id = :teamId
              AND tp.status = :participantStatus
              AND t.status IN :tournamentStatuses
            ORDER BY tp.registeredAt DESC
            """)
    List<TournamentParticipant> findActiveByTeamId(
            @Param("teamId") Long teamId,
            @Param("participantStatus") ParticipantStatus participantStatus,
            @Param("tournamentStatuses") Collection<TournamentStatus> tournamentStatuses);

    @Query("""
            SELECT tp
            FROM TournamentParticipant tp
            JOIN FETCH tp.tournament t
            WHERE tp.team.id = :teamId
              AND tp.status = :status
            """)
    List<TournamentParticipant> findByTeamIdAndStatusWithTournament(
            @Param("teamId") Long teamId,
            @Param("status") ParticipantStatus status);

    @Query("""
            SELECT t.preset.id, COUNT(tp)
            FROM TournamentParticipant tp
            JOIN tp.tournament t
            WHERE tp.team.id = :teamId
              AND tp.status = :participantStatus
              AND t.preset IS NOT NULL
            GROUP BY t.preset.id
            ORDER BY COUNT(tp) DESC, t.preset.id ASC
            """)
    List<Object[]> countApprovedParticipationsByPreset(@Param("teamId") Long teamId,
                                                       @Param("participantStatus") ParticipantStatus participantStatus);

    @Query("""
            SELECT tp
            FROM TournamentParticipant tp
            JOIN FETCH tp.tournament t
            LEFT JOIN FETCH t.preset
            JOIN tp.contact c
            WHERE c.userid = :clientUserId
              AND tp.status = :participantStatus
              AND t.status IN :tournamentStatuses
            ORDER BY tp.registeredAt DESC
            """)
    List<TournamentParticipant> findActiveByClientUserId(
            @Param("clientUserId") Integer clientUserId,
            @Param("participantStatus") ParticipantStatus participantStatus,
            @Param("tournamentStatuses") Collection<TournamentStatus> tournamentStatuses);

    @Query("""
            SELECT t.preset.id, COUNT(tp)
            FROM TournamentParticipant tp
            JOIN tp.tournament t
            JOIN tp.contact c
            WHERE c.userid = :clientUserId
              AND tp.status = :participantStatus
              AND t.preset IS NOT NULL
            GROUP BY t.preset.id
            ORDER BY COUNT(tp) DESC, t.preset.id ASC
            """)
    List<Object[]> countApprovedParticipationsByPresetForClient(
            @Param("clientUserId") Integer clientUserId,
            @Param("participantStatus") ParticipantStatus participantStatus);

    @Query("""
            SELECT COUNT(DISTINCT t.id)
            FROM TournamentParticipant tp
            JOIN tp.tournament t
            WHERE tp.status = :participantStatus
              AND t.status IN :tournamentStatuses
              AND (
                EXISTS (
                    SELECT 1 FROM TournamentParticipantPlayer tpp
                    WHERE tpp.participant = tp
                      AND tpp.client.userId = :clientUserId
                )
                OR (tp.contact IS NOT NULL AND tp.contact.userid = :clientUserId)
              )
            """)
    long countActiveParticipationsByClientUserId(
            @Param("clientUserId") Integer clientUserId,
            @Param("participantStatus") ParticipantStatus participantStatus,
            @Param("tournamentStatuses") Collection<TournamentStatus> tournamentStatuses);
}
