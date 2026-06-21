package com.arenagamer.api.repository;

import com.arenagamer.api.entity.CreditTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CreditTierRepository extends JpaRepository<CreditTier, Long> {

    List<CreditTier> findAllByOrderByMinParticipantsAsc();

    @Query("SELECT ct FROM CreditTier ct WHERE :participants BETWEEN ct.minParticipants AND ct.maxParticipants")
    Optional<CreditTier> findByParticipantCount(int participants);

    @Query("""
            SELECT COUNT(ct) > 0 FROM CreditTier ct
            WHERE (:excludeId IS NULL OR ct.id <> :excludeId)
              AND :min <= ct.maxParticipants
              AND :max >= ct.minParticipants
            """)
    boolean existsOverlappingRange(@Param("min") int min,
                                   @Param("max") int max,
                                   @Param("excludeId") Long excludeId);
}
