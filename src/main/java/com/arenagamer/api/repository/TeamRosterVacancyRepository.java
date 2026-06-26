package com.arenagamer.api.repository;

import com.arenagamer.api.entity.TeamRosterVacancy;
import com.arenagamer.api.entity.enums.TeamRosterVacancyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamRosterVacancyRepository extends JpaRepository<TeamRosterVacancy, Long> {

    Optional<TeamRosterVacancy> findByIdAndTeam_Id(Long id, Long teamId);

    @Query("""
            SELECT v FROM TeamRosterVacancy v
            JOIN FETCH v.tournament t
            JOIN FETCH v.exitedClient ec
            LEFT JOIN FETCH v.replacementClient rc
            WHERE v.team.id = :teamId
              AND (:status IS NULL OR v.status = :status)
            ORDER BY v.openedAt DESC
            """)
    List<TeamRosterVacancy> findByTeamIdWithDetails(
            @Param("teamId") Long teamId,
            @Param("status") TeamRosterVacancyStatus status);

    List<TeamRosterVacancy> findByParticipant_IdAndStatus(
            Long participantId, TeamRosterVacancyStatus status);

    List<TeamRosterVacancy> findByTournament_IdAndStatus(
            Long tournamentId, TeamRosterVacancyStatus status);
}
