package com.arenagamer.api.repository;

import com.arenagamer.api.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    Optional<TeamMember> findByTeamIdAndClient_UserId(Long teamId, Integer clientUserId);

    boolean existsByTeamIdAndClient_UserId(Long teamId, Integer clientUserId);

    boolean existsByTeamIdAndClient_UserIdAndIsCaptainTrue(Long teamId, Integer clientUserId);

    long countByClient_UserId(Integer clientUserId);

    long countByTeam_Id(Long teamId);

    void deleteByTeamIdAndClient_UserId(Long teamId, Integer clientUserId);

    @Query("""
            SELECT m
            FROM TeamMember m
            JOIN FETCH m.client
            WHERE m.team.id = :teamId
            ORDER BY m.isCaptain DESC, m.client.company
            """)
    List<TeamMember> findByTeamIdWithClient(@Param("teamId") Long teamId);
}
