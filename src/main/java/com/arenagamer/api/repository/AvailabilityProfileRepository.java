package com.arenagamer.api.repository;

import com.arenagamer.api.entity.AvailabilityProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AvailabilityProfileRepository extends JpaRepository<AvailabilityProfile, Long> {
    Optional<AvailabilityProfile> findByContact_Id(Integer contactId);
}
