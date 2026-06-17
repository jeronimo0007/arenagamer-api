package com.arenagamer.api.repository;

import com.arenagamer.api.entity.Tournament;
import com.arenagamer.api.entity.enums.TournamentStatus;
import com.arenagamer.api.entity.enums.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    Optional<Tournament> findBySlug(String slug);
    boolean existsBySlug(String slug);

    Page<Tournament> findByVisibilityAndStatusIn(Visibility visibility, java.util.Collection<TournamentStatus> statuses, Pageable pageable);

    Page<Tournament> findByOwnerId(Long ownerId, Pageable pageable);

    @Query("SELECT t FROM Tournament t JOIN TournamentParticipant tp ON tp.tournament = t WHERE tp.user.id = :userId")
    Page<Tournament> findJoinedByUserId(Long userId, Pageable pageable);
}
