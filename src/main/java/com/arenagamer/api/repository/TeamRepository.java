package com.arenagamer.api.repository;

import com.arenagamer.api.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByOwnerId(Long ownerId);
    List<Team> findByMembersUserId(Long userId);
}
