package com.arenagamer.api.repository;

import com.arenagamer.api.entity.UserSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    @Query("""
            SELECT s FROM UserSubscription s
            JOIN FETCH s.plan
            LEFT JOIN FETCH s.pendingPlan
            WHERE s.client.userId = :clientUserId
              AND s.active = true
              AND s.expiresAt > :now
            """)
    Optional<UserSubscription> findActiveByClientUserId(@Param("clientUserId") Integer clientUserId,
                                                          @Param("now") LocalDateTime now);

    @Query("""
            SELECT s FROM UserSubscription s
            JOIN FETCH s.plan
            LEFT JOIN FETCH s.pendingPlan
            JOIN FETCH s.client
            WHERE s.client.userId = :clientUserId
              AND s.active = true
              AND s.expiresAt <= :now
            """)
    List<UserSubscription> findDueActiveByClientUserId(@Param("clientUserId") Integer clientUserId,
                                                       @Param("now") LocalDateTime now);

    @Query("""
            SELECT s FROM UserSubscription s
            JOIN FETCH s.plan
            LEFT JOIN FETCH s.pendingPlan
            JOIN FETCH s.client
            WHERE s.client.userId = :clientUserId
              AND s.active = true
            """)
    List<UserSubscription> findAllActiveByClientUserId(@Param("clientUserId") Integer clientUserId);

    @EntityGraph(attributePaths = {"plan", "pendingPlan", "client"})
    @Query(
            value = """
            SELECT s FROM UserSubscription s
            JOIN s.client c
            WHERE s.active = true
              AND s.expiresAt > :now
              AND (:search IS NULL OR :search = ''
                   OR LOWER(c.company) LIKE LOWER(CONCAT('%', :search, '%')))
            """,
            countQuery = """
            SELECT COUNT(s) FROM UserSubscription s
            JOIN s.client c
            WHERE s.active = true
              AND s.expiresAt > :now
              AND (:search IS NULL OR :search = ''
                   OR LOWER(c.company) LIKE LOWER(CONCAT('%', :search, '%')))
            """
    )
    Page<UserSubscription> findActiveSubscriptions(@Param("now") LocalDateTime now,
                                                   @Param("search") String search,
                                                   Pageable pageable);

    @EntityGraph(attributePaths = {"plan", "pendingPlan", "client"})
    @Query(
            value = """
            SELECT DISTINCT s FROM UserSubscription s
            JOIN s.plan p
            LEFT JOIN s.pendingPlan pp
            JOIN s.client c
            WHERE s.active = true
              AND s.expiresAt > :now
              AND (p.id = :planId OR pp.id = :planId)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(c.company) LIKE LOWER(CONCAT('%', :search, '%')))
            """,
            countQuery = """
            SELECT COUNT(DISTINCT s) FROM UserSubscription s
            JOIN s.plan p
            LEFT JOIN s.pendingPlan pp
            JOIN s.client c
            WHERE s.active = true
              AND s.expiresAt > :now
              AND (p.id = :planId OR pp.id = :planId)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(c.company) LIKE LOWER(CONCAT('%', :search, '%')))
            """
    )
    Page<UserSubscription> findActiveSubscriptionsByPlan(@Param("now") LocalDateTime now,
                                                         @Param("planId") Long planId,
                                                         @Param("search") String search,
                                                         Pageable pageable);

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM UserSubscription s
            WHERE s.plan.id = :planId
              AND s.active = true
              AND s.expiresAt > :now
            """)
    boolean existsActiveSubscriptionByPlanId(@Param("planId") Long planId, @Param("now") LocalDateTime now);

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM UserSubscription s
            WHERE s.pendingPlan.id = :planId
              AND s.active = true
              AND s.expiresAt > :now
            """)
    boolean existsActivePendingPlanReference(@Param("planId") Long planId, @Param("now") LocalDateTime now);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE UserSubscription s SET s.pendingPlan = null WHERE s.pendingPlan.id = :planId")
    int clearPendingPlanReferences(@Param("planId") Long planId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM UserSubscription s WHERE s.plan.id = :planId")
    int deleteAllByPlanId(@Param("planId") Long planId);

    boolean existsByPlan_Id(Long planId);
}
