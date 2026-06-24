package com.arenagamer.api.repository;

import com.arenagamer.api.entity.Team;
import com.arenagamer.api.entity.enums.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByClient_UserId(Integer clientUserId);

    long countByClient_UserId(Integer clientUserId);

    boolean existsByClient_UserIdAndIdNot(Integer clientUserId, Long teamId);

    @Query("""
            SELECT DISTINCT t
            FROM Team t
            JOIN FETCH t.client
            LEFT JOIN FETCH t.members
            WHERE EXISTS (
                SELECT 1 FROM TeamMember m
                WHERE m.team = t AND m.client.userId = :clientUserId
            )
            """)
    List<Team> findByMemberClientUserIdWithDetails(@Param("clientUserId") Integer clientUserId);

    @Query("""
            SELECT t
            FROM Team t
            JOIN FETCH t.client
            LEFT JOIN FETCH t.members
            WHERE t.client.userId = :clientUserId
            """)
    List<Team> findOwnedByClientUserIdWithDetails(@Param("clientUserId") Integer clientUserId);

    @Query("""
            SELECT t
            FROM Team t
            JOIN FETCH t.client
            LEFT JOIN FETCH t.members
            WHERE t.id = :id
            """)
    Optional<Team> findByIdWithDetails(@Param("id") Long id);

    @Query(
            value = """
                    SELECT t
                    FROM Team t
                    JOIN FETCH t.client
                    WHERE t.visibility IN :visibilities
                      AND t.active = true
                    """,
            countQuery = """
                    SELECT COUNT(t)
                    FROM Team t
                    WHERE t.visibility IN :visibilities
                      AND t.active = true
                    """)
    Page<Team> findDiscoverableWithClient(
            @Param("visibilities") Collection<Visibility> visibilities,
            Pageable pageable);

    @Query("""
            SELECT t
            FROM Team t
            JOIN FETCH t.client
            WHERE t.id = :id
              AND t.visibility IN :visibilities
              AND t.active = true
            """)
    Optional<Team> findDiscoverableByIdWithClient(
            @Param("id") Long id,
            @Param("visibilities") Collection<Visibility> visibilities);

    @Query(
            value = """
                    SELECT t
                    FROM Team t
                    JOIN FETCH t.client
                    WHERE t.visibility = :visibility
                      AND t.active = true
                    """,
            countQuery = """
                    SELECT COUNT(t)
                    FROM Team t
                    WHERE t.visibility = :visibility
                      AND t.active = true
                    """)
    Page<Team> findPublicWithClient(@Param("visibility") Visibility visibility, Pageable pageable);

    @Query("""
            SELECT t
            FROM Team t
            JOIN FETCH t.client
            WHERE t.id = :id
              AND t.visibility IN :visibilities
              AND t.active = true
            """)
    Optional<Team> findDiscoverableById(@Param("id") Long id, @Param("visibilities") Collection<Visibility> visibilities);
}
