package com.arenagamer.api.repository;

import com.arenagamer.api.entity.TeamRank;
import com.arenagamer.api.entity.enums.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TeamRankRepository extends JpaRepository<TeamRank, Long> {

    @Query("""
            SELECT r
            FROM TeamRank r
            JOIN FETCH r.preset
            JOIN FETCH r.team t
            JOIN FETCH t.client
            WHERE r.team.id = :teamId
            ORDER BY r.preset.gameName
            """)
    List<TeamRank> findByTeamIdWithPreset(@Param("teamId") Long teamId);

    Optional<TeamRank> findByTeam_IdAndPreset_Id(Long teamId, Long presetId);

    @Query("""
            SELECT r
            FROM TeamRank r
            JOIN FETCH r.preset
            JOIN FETCH r.team t
            JOIN FETCH t.client
            WHERE r.team.id = :teamId
              AND r.preset.id = :presetId
            """)
    Optional<TeamRank> findByTeamIdAndPresetIdWithDetails(
            @Param("teamId") Long teamId,
            @Param("presetId") Long presetId);

    void deleteByTeam_IdAndPreset_IdNotIn(Long teamId, Collection<Long> presetIds);

    void deleteByTeam_Id(Long teamId);

    @Query("""
            SELECT r
            FROM TeamRank r
            JOIN FETCH r.team t
            JOIN FETCH t.client
            JOIN FETCH r.preset p
            WHERE p.id = :presetId
              AND t.active = true
              AND t.visibility IN :visibilities
            ORDER BY r.rankPoints DESC, t.id ASC
            """)
    Page<TeamRank> findPublicLeaderboard(
            @Param("presetId") Long presetId,
            @Param("visibilities") Collection<Visibility> visibilities,
            Pageable pageable);

    @Query("""
            SELECT r
            FROM TeamRank r
            JOIN FETCH r.team t
            JOIN FETCH t.client c
            JOIN FETCH r.preset p
            WHERE p.id = :presetId
              AND t.active = true
              AND t.visibility IN :visibilities
              AND c.state = :state
            ORDER BY r.rankPoints DESC, t.id ASC
            """)
    Page<TeamRank> findPublicRegionalLeaderboard(
            @Param("presetId") Long presetId,
            @Param("visibilities") Collection<Visibility> visibilities,
            @Param("state") String state,
            Pageable pageable);

    @Query("""
            SELECT COUNT(r) + 1
            FROM TeamRank r
            JOIN r.team t
            WHERE r.preset.id = :presetId
              AND t.active = true
              AND t.visibility IN :visibilities
              AND (r.rankPoints > :rankPoints
                   OR (r.rankPoints = :rankPoints AND t.id < :teamId))
            """)
    long countGlobalPosition(
            @Param("presetId") Long presetId,
            @Param("visibilities") Collection<Visibility> visibilities,
            @Param("rankPoints") Integer rankPoints,
            @Param("teamId") Long teamId);

    @Query("""
            SELECT COUNT(r) + 1
            FROM TeamRank r
            JOIN r.team t
            JOIN t.client c
            WHERE r.preset.id = :presetId
              AND t.active = true
              AND t.visibility IN :visibilities
              AND c.state = :state
              AND (r.rankPoints > :rankPoints
                   OR (r.rankPoints = :rankPoints AND t.id < :teamId))
            """)
    long countRegionalPosition(
            @Param("presetId") Long presetId,
            @Param("visibilities") Collection<Visibility> visibilities,
            @Param("state") String state,
            @Param("rankPoints") Integer rankPoints,
            @Param("teamId") Long teamId);

    @Query("""
            SELECT r
            FROM TeamRank r
            JOIN FETCH r.team t
            JOIN FETCH t.client
            JOIN FETCH r.preset p
            WHERE t.client.userId = :clientUserId
              AND t.active = true
              AND (:presetId IS NULL OR p.id = :presetId)
            ORDER BY p.gameName, t.name
            """)
    List<TeamRank> findByClientUserIdAndOptionalPreset(
            @Param("clientUserId") Integer clientUserId,
            @Param("presetId") Long presetId);
}
