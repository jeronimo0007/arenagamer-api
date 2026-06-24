package com.arenagamer.api.repository;

import com.arenagamer.api.entity.ClientRank;
import com.arenagamer.api.entity.enums.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClientRankRepository extends JpaRepository<ClientRank, Long> {

    @Query("""
            SELECT r
            FROM ClientRank r
            JOIN FETCH r.preset
            JOIN FETCH r.client c
            WHERE c.userId = :clientUserId
            ORDER BY r.preset.gameName
            """)
    List<ClientRank> findByClientUserIdWithPreset(@Param("clientUserId") Integer clientUserId);

    Optional<ClientRank> findByClient_UserIdAndPreset_Id(Integer clientUserId, Long presetId);

    @Query("""
            SELECT r
            FROM ClientRank r
            JOIN FETCH r.preset
            JOIN FETCH r.client c
            WHERE c.userId = :clientUserId
              AND r.preset.id = :presetId
            """)
    Optional<ClientRank> findByClientUserIdAndPresetIdWithDetails(
            @Param("clientUserId") Integer clientUserId,
            @Param("presetId") Long presetId);

    void deleteByClient_UserIdAndPreset_IdNotIn(Integer clientUserId, Collection<Long> presetIds);

    void deleteByClient_UserId(Integer clientUserId);

    @Query("""
            SELECT COUNT(r) + 1
            FROM ClientRank r
            JOIN r.client c
            WHERE r.preset.id = :presetId
              AND c.active = 1
              AND c.visibility IN :visibilities
              AND (r.rankPoints > :rankPoints
                   OR (r.rankPoints = :rankPoints AND c.userId < :clientUserId))
            """)
    long countGlobalPosition(
            @Param("presetId") Long presetId,
            @Param("visibilities") Collection<Visibility> visibilities,
            @Param("rankPoints") Integer rankPoints,
            @Param("clientUserId") Integer clientUserId);

    @Query("""
            SELECT COUNT(r) + 1
            FROM ClientRank r
            JOIN r.client c
            WHERE r.preset.id = :presetId
              AND c.active = 1
              AND c.visibility IN :visibilities
              AND c.state = :state
              AND (r.rankPoints > :rankPoints
                   OR (r.rankPoints = :rankPoints AND c.userId < :clientUserId))
            """)
    long countRegionalPosition(
            @Param("presetId") Long presetId,
            @Param("visibilities") Collection<Visibility> visibilities,
            @Param("state") String state,
            @Param("rankPoints") Integer rankPoints,
            @Param("clientUserId") Integer clientUserId);

    @Query("""
            SELECT r
            FROM ClientRank r
            JOIN FETCH r.client c
            JOIN FETCH r.preset p
            WHERE p.id = :presetId
              AND c.active = 1
              AND c.visibility IN :visibilities
            ORDER BY r.rankPoints DESC, c.userId ASC
            """)
    Page<ClientRank> findPublicLeaderboard(
            @Param("presetId") Long presetId,
            @Param("visibilities") Collection<Visibility> visibilities,
            Pageable pageable);

    @Query("""
            SELECT r
            FROM ClientRank r
            JOIN FETCH r.client c
            JOIN FETCH r.preset p
            WHERE p.id = :presetId
              AND c.active = 1
              AND c.visibility IN :visibilities
              AND c.state = :state
            ORDER BY r.rankPoints DESC, c.userId ASC
            """)
    Page<ClientRank> findPublicRegionalLeaderboard(
            @Param("presetId") Long presetId,
            @Param("visibilities") Collection<Visibility> visibilities,
            @Param("state") String state,
            Pageable pageable);
}
