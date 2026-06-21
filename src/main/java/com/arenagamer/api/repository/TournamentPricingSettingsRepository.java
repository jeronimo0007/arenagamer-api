package com.arenagamer.api.repository;

import com.arenagamer.api.entity.TournamentPricingSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentPricingSettingsRepository extends JpaRepository<TournamentPricingSettings, Long> {
}
