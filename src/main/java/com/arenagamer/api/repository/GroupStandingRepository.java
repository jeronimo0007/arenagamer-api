package com.arenagamer.api.repository;

import com.arenagamer.api.entity.GroupStanding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupStandingRepository extends JpaRepository<GroupStanding, Long> {
    List<GroupStanding> findByTournamentIdAndGroupNumberOrderByPointsDescGoalDifferenceDesc(Long tournamentId, Integer groupNumber);
    List<GroupStanding> findByTournamentIdOrderByGroupNumberAscPointsDescGoalDifferenceDesc(Long tournamentId);
}
