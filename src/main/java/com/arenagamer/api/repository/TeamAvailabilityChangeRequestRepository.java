package com.arenagamer.api.repository;

import com.arenagamer.api.entity.TeamAvailabilityChangeRequest;
import com.arenagamer.api.entity.enums.AvailabilityChangeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamAvailabilityChangeRequestRepository extends JpaRepository<TeamAvailabilityChangeRequest, Long> {

    List<TeamAvailabilityChangeRequest> findByTeam_IdOrderByCreatedAtDesc(Long teamId);

    List<TeamAvailabilityChangeRequest> findByTeam_IdAndStatusOrderByCreatedAtDesc(
            Long teamId, AvailabilityChangeStatus status);

    Optional<TeamAvailabilityChangeRequest> findByIdAndTeam_Id(Long id, Long teamId);

    boolean existsByTeam_IdAndStatus(Long teamId, AvailabilityChangeStatus status);

    void deleteByTeam_Id(Long teamId);
}
