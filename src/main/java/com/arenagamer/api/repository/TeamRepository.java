package com.arenagamer.api.repository;

import com.arenagamer.api.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByOwner_Id(Integer ownerContactId);

    List<Team> findByMembers_Contact_Id(Integer contactId);

    @Query("""
            SELECT DISTINCT t
            FROM Team t
            JOIN FETCH t.owner
            LEFT JOIN FETCH t.members
            WHERE EXISTS (
                SELECT 1 FROM TeamMember m
                WHERE m.team = t AND m.contact.id = :contactId
            )
            """)
    List<Team> findByMemberContactIdWithDetails(@Param("contactId") Integer contactId);

    @Query("""
            SELECT t
            FROM Team t
            JOIN FETCH t.owner
            LEFT JOIN FETCH t.members
            WHERE t.id = :id
            """)
    Optional<Team> findByIdWithDetails(@Param("id") Long id);

    long countByOwner_Id(Integer ownerContactId);

    boolean existsByOwner_IdAndIdNot(Integer ownerContactId, Long teamId);
}
