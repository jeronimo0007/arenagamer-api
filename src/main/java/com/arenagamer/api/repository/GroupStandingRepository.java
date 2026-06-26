package com.arenagamer.api.repository;

import com.arenagamer.api.entity.GroupStanding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupStandingRepository extends JpaRepository<GroupStanding, Long> {
    List<GroupStanding> findByTournamentIdAndGroupNumberOrderByPointsDescGoalDifferenceDesc(Long tournamentId, Integer groupNumber);
    List<GroupStanding> findByTournamentIdOrderByGroupNumberAscPointsDescGoalDifferenceDesc(Long tournamentId);
    Optional<GroupStanding> findByTournamentIdAndParticipantId(Long tournamentId, Long participantId);
}
