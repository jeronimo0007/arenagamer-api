package com.arenagamer.api.repository;

import com.arenagamer.api.entity.CreditTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CreditTierRepository extends JpaRepository<CreditTier, Long> {
    @Query("SELECT ct FROM CreditTier ct WHERE :participants BETWEEN ct.minParticipants AND ct.maxParticipants")
    Optional<CreditTier> findByParticipantCount(int participants);
}
