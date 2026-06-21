package com.arenagamer.api.repository;

import com.arenagamer.api.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    Optional<TeamMember> findByTeamIdAndContactId(Long teamId, Integer contactId);
    boolean existsByTeamIdAndContactId(Long teamId, Integer contactId);
    boolean existsByContact_Id(Integer contactId);
    long countByContact_Id(Integer contactId);
}
