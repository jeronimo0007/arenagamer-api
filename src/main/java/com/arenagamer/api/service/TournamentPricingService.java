package com.arenagamer.api.service;

import com.arenagamer.api.dto.request.TournamentPricingRequest;
import com.arenagamer.api.entity.TournamentPricingSettings;
import com.arenagamer.api.repository.TournamentPricingSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TournamentPricingService {

    private final TournamentPricingSettingsRepository repository;
    private final AuditService auditService;

    @Cacheable("tournamentPricing")
    public TournamentPricingSettings getSettings() {
        return repository.findById(TournamentPricingSettings.SINGLETON_ID)
                .orElseGet(this::createDefaultSettings);
    }

    @Transactional
    @CacheEvict(value = "tournamentPricing", allEntries = true)
    public TournamentPricingSettings update(TournamentPricingRequest request) {
        TournamentPricingSettings settings = getSettings();
        settings.setBaseTournamentPrice(request.getBaseTournamentPrice());
        settings.setExtraParticipantPrice(request.getExtraParticipantPrice());
        settings.setIncludedParticipants(request.getIncludedParticipants());

        TournamentPricingSettings saved = repository.save(settings);
        auditService.recordStaffAction("UPDATE", "tournament_pricing", saved.getId(), null, request);
        return saved;
    }

    public BigDecimal calculateCreationCost(int participantsLimit) {
        TournamentPricingSettings settings = getSettings();
        int included = settings.getIncludedParticipants();
        BigDecimal cost = settings.getBaseTournamentPrice();

        if (participantsLimit > included) {
            int extraParticipants = participantsLimit - included;
            cost = cost.add(settings.getExtraParticipantPrice()
                    .multiply(BigDecimal.valueOf(extraParticipants)));
        }

        return cost;
    }

    private TournamentPricingSettings createDefaultSettings() {
        TournamentPricingSettings defaults = TournamentPricingSettings.builder()
                .id(TournamentPricingSettings.SINGLETON_ID)
                .baseTournamentPrice(new BigDecimal("5.00"))
                .extraParticipantPrice(new BigDecimal("1.00"))
                .includedParticipants(8)
                .build();
        return repository.save(defaults);
    }
}
