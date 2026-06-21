package com.arenagamer.api.service;

import com.arenagamer.api.dto.request.TeamSettingsRequest;
import com.arenagamer.api.entity.TeamSettings;
import com.arenagamer.api.repository.TeamSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamSettingsService {

    private final TeamSettingsRepository repository;
    private final AuditService auditService;

    @Cacheable("teamSettings")
    public TeamSettings getSettings() {
        return repository.findById(TeamSettings.SINGLETON_ID)
                .orElseGet(this::createDefaultSettings);
    }

    @Transactional
    @CacheEvict(value = "teamSettings", allEntries = true)
    public TeamSettings update(TeamSettingsRequest request) {
        TeamSettings settings = getSettings();
        settings.setMaxOwnedTeamsPerContact(request.getMaxOwnedTeamsPerContact());
        settings.setMaxParticipatedTeamsPerContact(request.getMaxParticipatedTeamsPerContact());
        settings.setMaxTournamentsPerTeam(request.getMaxTournamentsPerTeam());

        TeamSettings saved = repository.save(settings);
        auditService.recordStaffAction("UPDATE", "team_settings", saved.getId(), null, request);
        return saved;
    }

    private TeamSettings createDefaultSettings() {
        TeamSettings defaults = TeamSettings.builder()
                .id(TeamSettings.SINGLETON_ID)
                .maxOwnedTeamsPerContact(1)
                .maxParticipatedTeamsPerContact(3)
                .maxTournamentsPerTeam(null)
                .build();
        return repository.save(defaults);
    }
}
