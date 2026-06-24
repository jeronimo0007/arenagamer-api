package com.arenagamer.api.repository;

import com.arenagamer.api.entity.TournamentEntryFee;
import com.arenagamer.api.entity.enums.EntryFeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TournamentEntryFeeRepository extends JpaRepository<TournamentEntryFee, Long> {

    Optional<TournamentEntryFee> findByParticipantId(Long participantId);

    List<TournamentEntryFee> findByTournamentIdAndStatus(Long tournamentId, EntryFeeStatus status);

    @Query("""
            SELECT f FROM TournamentEntryFee f
            JOIN FETCH f.participant p
            LEFT JOIN FETCH p.team t
            LEFT JOIN FETCH p.contact c
            LEFT JOIN FETCH c.client cl
            WHERE f.tournament.id = :tournamentId
            ORDER BY f.createdAt DESC
            """)
    List<TournamentEntryFee> findByTournamentIdWithDetails(@Param("tournamentId") Long tournamentId);

    @Query("""
            SELECT COALESCE(SUM(f.amount), 0) FROM TournamentEntryFee f
            WHERE f.tournament.id = :tournamentId AND f.status = :status
            """)
    BigDecimal sumAmountByTournamentIdAndStatus(
            @Param("tournamentId") Long tournamentId,
            @Param("status") EntryFeeStatus status);
}
