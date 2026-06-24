package com.arenagamer.api.service;

import com.arenagamer.api.dto.request.PresetRequest;
import com.arenagamer.api.entity.Preset;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.PresetRepository;
import com.arenagamer.api.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PresetService {

    private final PresetRepository presetRepository;
    private final TournamentRepository tournamentRepository;
    private final AuditService auditService;

    @Cacheable("presets")
    public List<Preset> listActive() {
        return presetRepository.findByActiveTrueOrderByGameNameAsc();
    }

    @Cacheable("adminPresets")
    public List<Preset> listAll() {
        return presetRepository.findAll().stream()
                .sorted((a, b) -> a.getGameName().compareToIgnoreCase(b.getGameName()))
                .toList();
    }

    public List<Preset> search(String query, boolean activeOnly) {
        String normalized = normalizeSearchQuery(query);
        if (normalized == null) {
            return activeOnly ? listActive() : listAll();
        }
        if (normalized.length() < 3) {
            return List.of();
        }
        return presetRepository.searchByText(normalized, activeOnly);
    }

    public Preset getById(Long id) {
        return presetRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Preset não encontrado"));
    }

    @Transactional
    @CacheEvict(value = {"presets", "adminPresets"}, allEntries = true)
    public Preset create(PresetRequest request) {
        validatePlayersRange(request);

        Preset preset = Preset.builder()
                .gameName(request.getGameName().trim())
                .platform(normalizeText(request.getPlatform()))
                .teamSize(request.getTeamSize())
                .minPlayersPerTeam(request.getMinPlayersPerTeam())
                .maxPlayersPerTeam(request.getMaxPlayersPerTeam())
                .iconUrl(normalizeUrl(request.getIconUrl()))
                .gameImageUrl(resolvePresetGameImageUrl(request))
                .rulesTemplate(normalizeText(request.getRulesTemplate()))
                .scoringScript(normalizeText(request.getScoringScript()))
                .active(request.getActive() == null || request.getActive())
                .build();

        Preset saved = presetRepository.save(preset);
        auditService.recordStaffAction("CREATE", "preset", saved.getId(), null, request);
        return saved;
    }

    @Transactional
    @CacheEvict(value = {"presets", "adminPresets"}, allEntries = true)
    public Preset update(Long id, PresetRequest request) {
        validatePlayersRange(request);

        Preset preset = getById(id);
        preset.setGameName(request.getGameName().trim());
        preset.setPlatform(normalizeText(request.getPlatform()));
        preset.setTeamSize(request.getTeamSize());
        preset.setMinPlayersPerTeam(request.getMinPlayersPerTeam());
        preset.setMaxPlayersPerTeam(request.getMaxPlayersPerTeam());
        preset.setIconUrl(normalizeUrl(request.getIconUrl()));
        preset.setGameImageUrl(resolvePresetGameImageUrl(request));
        preset.setRulesTemplate(normalizeText(request.getRulesTemplate()));
        preset.setScoringScript(normalizeText(request.getScoringScript()));
        preset.setActive(request.getActive() == null || request.getActive());

        Preset saved = presetRepository.save(preset);
        tournamentRepository.updateGameNameByPresetId(saved.getId(), saved.getGameName());
        auditService.recordStaffAction("UPDATE", "preset", saved.getId(), null, request);
        return saved;
    }

    private void validatePlayersRange(PresetRequest request) {
        if (request.getMinPlayersPerTeam() > request.getMaxPlayersPerTeam()) {
            throw BusinessException.badRequest("Mínimo de jogadores não pode ser maior que o máximo");
        }
        if (request.getTeamSize() < request.getMinPlayersPerTeam()
                || request.getTeamSize() > request.getMaxPlayersPerTeam()) {
            throw BusinessException.badRequest("Tamanho do time deve estar entre o mínimo e o máximo de jogadores");
        }
    }

    private String resolvePresetGameImageUrl(PresetRequest request) {
        String gameImage = normalizeUrl(request.getGameImageUrl());
        if (gameImage != null) {
            return gameImage;
        }
        return normalizeUrl(request.getIconUrl());
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeUrl(String value) {
        return normalizeText(value);
    }

    private String normalizeSearchQuery(String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
