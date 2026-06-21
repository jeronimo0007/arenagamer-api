package com.arenagamer.api.service;

import com.arenagamer.api.dto.request.PlanRequest;
import com.arenagamer.api.entity.Plan;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.PlanRepository;
import com.arenagamer.api.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final AuditService auditService;

    @Cacheable("plans")
    public List<Plan> listAll() {
        return planRepository.findAll();
    }

    @Cacheable("publicPlans")
    public List<Plan> listPublic() {
        return planRepository.findByActiveTrueAndHiddenFalseOrderBySortOrder();
    }

    public Plan getById(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Plano não encontrado"));
    }

    @Transactional
    @CacheEvict(value = {"plans", "publicPlans"}, allEntries = true)
    public Plan create(PlanRequest request) {
        int maxTournamentsPerMonth = resolveMaxTournamentsPerMonth(request);
        Plan plan = Plan.builder()
                .name(request.getName())
                .description(request.getDescription())
                .freeTournamentsPerMonth(maxTournamentsPerMonth)
                .freeMaxParticipants(request.getFreeMaxParticipants())
                .allowsEntryFee(Boolean.TRUE.equals(request.getAllowsEntryFee()))
                .maxTournamentsPerMonth(maxTournamentsPerMonth)
                .monthlyPrice(request.getMonthlyPrice())
                .hidden(Boolean.TRUE.equals(request.getHidden()))
                .active(request.getActive() == null || request.getActive())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        Plan saved = planRepository.save(plan);
        auditService.recordStaffAction("CREATE", "plan", saved.getId(), null, request);
        return saved;
    }

    @Transactional
    @CacheEvict(value = {"plans", "publicPlans"}, allEntries = true)
    public Plan update(Long id, PlanRequest request) {
        Plan plan = getById(id);
        int maxTournamentsPerMonth = resolveMaxTournamentsPerMonth(request);

        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setFreeTournamentsPerMonth(maxTournamentsPerMonth);
        plan.setFreeMaxParticipants(request.getFreeMaxParticipants());
        plan.setAllowsEntryFee(Boolean.TRUE.equals(request.getAllowsEntryFee()));
        plan.setMaxTournamentsPerMonth(maxTournamentsPerMonth);
        plan.setMonthlyPrice(request.getMonthlyPrice());
        plan.setHidden(Boolean.TRUE.equals(request.getHidden()));
        plan.setActive(request.getActive() == null || request.getActive());
        plan.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

        Plan saved = planRepository.save(plan);
        auditService.recordStaffAction("UPDATE", "plan", saved.getId(), null, request);
        return saved;
    }

    @Transactional
    @CacheEvict(value = {"plans", "publicPlans"}, allEntries = true)
    public void delete(Long id) {
        Plan plan = getById(id);
        LocalDateTime now = LocalDateTime.now();

        if (userSubscriptionRepository.existsActiveSubscriptionByPlanId(id, now)) {
            throw BusinessException.conflict("Não é possível remover: existem clientes com assinatura ativa neste plano");
        }

        if (userSubscriptionRepository.existsActivePendingPlanReference(id, now)) {
            throw BusinessException.conflict("Não é possível remover: existem assinaturas agendadas para este plano");
        }

        userSubscriptionRepository.clearPendingPlanReferences(id);
        userSubscriptionRepository.deleteAllByPlanId(id);

        auditService.recordStaffMessage("DELETE", "plan", id, "Plano removido: " + plan.getName());
        planRepository.delete(plan);
    }

    private int resolveMaxTournamentsPerMonth(PlanRequest request) {
        if (request.getFreeTournamentsPerMonth() != null && request.getFreeTournamentsPerMonth() > 0) {
            return request.getFreeTournamentsPerMonth();
        }

        if (request.getMaxTournamentsPerMonth() != null && request.getMaxTournamentsPerMonth() > 0) {
            return request.getMaxTournamentsPerMonth();
        }

        return request.getFreeTournamentsPerMonth() != null ? request.getFreeTournamentsPerMonth() : 0;
    }
}
