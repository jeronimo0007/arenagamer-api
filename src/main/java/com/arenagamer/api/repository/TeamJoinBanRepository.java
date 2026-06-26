package com.arenagamer.api.repository;

import com.arenagamer.api.entity.TeamJoinBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TeamJoinBanRepository extends JpaRepository<TeamJoinBan, Long> {

    @Query("""
            SELECT b FROM TeamJoinBan b
            WHERE b.client.userId = :clientUserId
              AND b.bannedUntil > :now
            ORDER BY b.bannedUntil DESC
            """)
    Optional<TeamJoinBan> findActiveByClientUserId(
            @Param("clientUserId") Integer clientUserId,
            @Param("now") LocalDateTime now);
}
