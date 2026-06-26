package com.arenagamer.api.repository;

import com.arenagamer.api.entity.TeamJoinRequest;
import com.arenagamer.api.entity.enums.TeamJoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamJoinRequestRepository extends JpaRepository<TeamJoinRequest, Long> {

    boolean existsByTeam_IdAndClient_UserIdAndStatus(
            Long teamId, Integer clientUserId, TeamJoinRequestStatus status);

    List<TeamJoinRequest> findByTeam_IdAndClient_UserIdAndStatus(
            Long teamId, Integer clientUserId, TeamJoinRequestStatus status);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE TeamJoinRequest r
            SET r.status = :newStatus, r.resolvedAt = CURRENT_TIMESTAMP
            WHERE r.team.id = :teamId
              AND r.client.userId = :clientUserId
              AND r.status = :currentStatus
            """)
    int updateStatusByTeamAndClient(
            @Param("teamId") Long teamId,
            @Param("clientUserId") Integer clientUserId,
            @Param("currentStatus") TeamJoinRequestStatus currentStatus,
            @Param("newStatus") TeamJoinRequestStatus newStatus);

    Optional<TeamJoinRequest> findByIdAndClient_UserId(Long id, Integer clientUserId);

    @Query("""
            SELECT r FROM TeamJoinRequest r
            JOIN FETCH r.team t
            JOIN FETCH r.client c
            JOIN FETCH r.invitedBy ib
            WHERE r.id = :id AND r.client.userId = :clientUserId
            """)
    Optional<TeamJoinRequest> findByIdAndClient_UserIdWithDetails(
            @Param("id") Long id,
            @Param("clientUserId") Integer clientUserId);

    @Query("""
            SELECT r FROM TeamJoinRequest r
            JOIN FETCH r.team t
            JOIN FETCH t.client tc
            JOIN FETCH r.client c
            JOIN FETCH r.invitedBy ib
            WHERE r.team.id = :teamId
              AND (:status IS NULL OR r.status = :status)
            ORDER BY r.createdAt DESC
            """)
    List<TeamJoinRequest> findByTeamIdWithDetails(
            @Param("teamId") Long teamId,
            @Param("status") TeamJoinRequestStatus status);

    @Query("""
            SELECT r FROM TeamJoinRequest r
            JOIN FETCH r.team t
            JOIN FETCH t.client tc
            JOIN FETCH r.client c
            JOIN FETCH r.invitedBy ib
            WHERE r.client.userId = :clientUserId
              AND (:status IS NULL OR r.status = :status)
            ORDER BY r.createdAt DESC
            """)
    List<TeamJoinRequest> findByClientUserIdWithDetails(
            @Param("clientUserId") Integer clientUserId,
            @Param("status") TeamJoinRequestStatus status);

    void deleteByTeam_Id(Long teamId);
}
