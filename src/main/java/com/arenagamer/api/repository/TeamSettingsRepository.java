package com.arenagamer.api.repository;

import com.arenagamer.api.entity.TeamSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamSettingsRepository extends JpaRepository<TeamSettings, Long> {
}
