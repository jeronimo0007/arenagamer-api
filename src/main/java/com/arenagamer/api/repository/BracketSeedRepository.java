package com.arenagamer.api.repository;

import com.arenagamer.api.entity.BracketSeed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BracketSeedRepository extends JpaRepository<BracketSeed, Long> {
    List<BracketSeed> findByTournamentIdOrderBySeedNumber(Long tournamentId);
}
