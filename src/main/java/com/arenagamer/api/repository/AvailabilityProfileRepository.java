package com.arenagamer.api.repository;

import com.arenagamer.api.entity.AvailabilityProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AvailabilityProfileRepository extends JpaRepository<AvailabilityProfile, Long> {
    Optional<AvailabilityProfile> findByContact_Id(Integer contactId);

    @Query("SELECT p FROM AvailabilityProfile p LEFT JOIN FETCH p.weeklySlots WHERE p.clientUserId = :clientUserId")
    Optional<AvailabilityProfile> findByClientUserId(@Param("clientUserId") Integer clientUserId);

    @Query("SELECT p FROM AvailabilityProfile p LEFT JOIN FETCH p.weeklySlots WHERE p.team.id = :teamId")
    Optional<AvailabilityProfile> findByTeam_Id(@Param("teamId") Long teamId);
}
