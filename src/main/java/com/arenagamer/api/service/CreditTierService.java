package com.arenagamer.api.service;

import com.arenagamer.api.dto.request.CreditTierRequest;
import com.arenagamer.api.entity.CreditTier;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.CreditTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditTierService {

    private final CreditTierRepository creditTierRepository;
    private final AuditService auditService;

    @Cacheable("creditTiers")
    public List<CreditTier> listAll() {
        return creditTierRepository.findAllByOrderByMinParticipantsAsc();
    }

    public CreditTier getById(Long id) {
        return creditTierRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Tier de créditos não encontrado"));
    }

    @Transactional
    @CacheEvict(value = "creditTiers", allEntries = true)
    public CreditTier create(CreditTierRequest request) {
        validateRange(request);
        if (creditTierRepository.existsOverlappingRange(
                request.getMinParticipants(), request.getMaxParticipants(), null)) {
            throw BusinessException.conflict("Faixa de participantes sobrepõe um tier existente");
        }

        CreditTier tier = CreditTier.builder()
                .minParticipants(request.getMinParticipants())
                .maxParticipants(request.getMaxParticipants())
                .creditCost(request.getCreditCost())
                .build();

        CreditTier saved = creditTierRepository.save(tier);
        auditService.recordStaffAction("CREATE", "credit_tier", saved.getId(), null, request);
        return saved;
    }

    @Transactional
    @CacheEvict(value = "creditTiers", allEntries = true)
    public CreditTier update(Long id, CreditTierRequest request) {
        validateRange(request);
        if (creditTierRepository.existsOverlappingRange(
                request.getMinParticipants(), request.getMaxParticipants(), id)) {
            throw BusinessException.conflict("Faixa de participantes sobrepõe um tier existente");
        }

        CreditTier tier = getById(id);
        tier.setMinParticipants(request.getMinParticipants());
        tier.setMaxParticipants(request.getMaxParticipants());
        tier.setCreditCost(request.getCreditCost());

        CreditTier saved = creditTierRepository.save(tier);
        auditService.recordStaffAction("UPDATE", "credit_tier", saved.getId(), null, request);
        return saved;
    }

    @Transactional
    @CacheEvict(value = "creditTiers", allEntries = true)
    public void delete(Long id) {
        CreditTier tier = getById(id);
        auditService.recordStaffMessage("DELETE", "credit_tier", id,
                "Tier removido: " + tier.getMinParticipants() + "-" + tier.getMaxParticipants());
        creditTierRepository.delete(tier);
    }

    private void validateRange(CreditTierRequest request) {
        if (request.getMaxParticipants() < request.getMinParticipants()) {
            throw BusinessException.badRequest("Máximo de participantes deve ser maior ou igual ao mínimo");
        }
    }
}
