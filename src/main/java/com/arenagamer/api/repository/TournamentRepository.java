package com.arenagamer.api.repository;

import com.arenagamer.api.entity.Tournament;
import com.arenagamer.api.entity.enums.AuthUserType;
import com.arenagamer.api.entity.enums.TournamentStatus;
import com.arenagamer.api.entity.enums.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    @EntityGraph(attributePaths = {"preset", "client"})
    Optional<Tournament> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = {"preset", "client"})
    @Override
    Page<Tournament> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"preset", "client"})
    Page<Tournament> findByVisibilityAndStatusIn(Visibility visibility, java.util.Collection<TournamentStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = {"preset", "client"})
    Page<Tournament> findByVisibilityAndStatus(Visibility visibility, TournamentStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"preset", "client"})
    @Query("""
            SELECT t FROM Tournament t
            WHERE t.visibility = com.arenagamer.api.entity.enums.Visibility.PUBLIC
              AND t.registrationOpensAt IS NOT NULL
            """)
    Page<Tournament> findPublicWithRegistrationOpensAt(Pageable pageable);

    @EntityGraph(attributePaths = {"preset", "client"})
    Page<Tournament> findByOwnerTypeAndOwnerId(AuthUserType ownerType, Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"preset", "client"})
    Page<Tournament> findByClient_UserId(Integer clientUserId, Pageable pageable);

    @EntityGraph(attributePaths = {"preset", "client"})
    @Query("SELECT t FROM Tournament t JOIN TournamentParticipant tp ON tp.tournament = t WHERE tp.contact.id = :contactId")
    Page<Tournament> findJoinedByContactId(Integer contactId, Pageable pageable);

    @EntityGraph(attributePaths = {"preset", "client"})
    @Query("""
            SELECT DISTINCT t FROM Tournament t
            LEFT JOIN TournamentManagerPermission tmp ON tmp.tournament = t
            WHERE (t.ownerType = com.arenagamer.api.entity.enums.AuthUserType.CONTACT AND t.ownerId = :contactIdLong)
               OR tmp.contact.id = :contactId
            """)
    Page<Tournament> findManagedByNonPrimaryContact(@Param("contactId") Integer contactId,
                                                    @Param("contactIdLong") Long contactIdLong,
                                                    Pageable pageable);

    @Query("""
            SELECT COUNT(t) FROM Tournament t
            WHERE t.client.userId = :clientUserId
              AND t.createdAt >= :monthStart
              AND t.createdAt < :monthEnd
              AND t.status <> com.arenagamer.api.entity.enums.TournamentStatus.CANCELLED
            """)
    long countByClientUserIdInCurrentMonth(@Param("clientUserId") Integer clientUserId,
                                           @Param("monthStart") LocalDateTime monthStart,
                                           @Param("monthEnd") LocalDateTime monthEnd);

    @Modifying
    @Query("UPDATE Tournament t SET t.gameName = :gameName WHERE t.preset.id = :presetId")
    void updateGameNameByPresetId(@Param("presetId") Long presetId, @Param("gameName") String gameName);
}
