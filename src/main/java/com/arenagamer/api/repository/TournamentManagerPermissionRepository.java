package com.arenagamer.api.repository;

import com.arenagamer.api.entity.TournamentManagerPermission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TournamentManagerPermissionRepository extends JpaRepository<TournamentManagerPermission, Long> {

    boolean existsByTournamentIdAndContactId(Long tournamentId, Integer contactId);

    Optional<TournamentManagerPermission> findByTournamentIdAndContactId(Long tournamentId, Integer contactId);

    @EntityGraph(attributePaths = {"contact"})
    List<TournamentManagerPermission> findByTournamentId(Long tournamentId);

    void deleteByTournamentIdAndContactId(Long tournamentId, Integer contactId);
}
