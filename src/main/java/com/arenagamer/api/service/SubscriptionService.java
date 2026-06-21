package com.arenagamer.api.service;

import com.arenagamer.api.dto.response.AdminSubscriptionResponse;
import com.arenagamer.api.dto.response.UserPlanResponse;
import com.arenagamer.api.entity.Client;
import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.Plan;
import com.arenagamer.api.entity.UserSubscription;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.ClientRepository;
import com.arenagamer.api.repository.ContactRepository;
import com.arenagamer.api.repository.PlanRepository;
import com.arenagamer.api.repository.TournamentRepository;
import com.arenagamer.api.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final PlanRepository planRepository;
    private final ContactRepository contactRepository;
    private final ClientRepository clientRepository;
    private final TournamentRepository tournamentRepository;
    private final WalletService walletService;
    private final AuditService auditService;

    public UserPlanResponse getActivePlanForClient(Integer clientUserId) {
        if (clientUserId == null) {
            return null;
        }

        processDueSubscriptionChanges(clientUserId);

        return findActiveSubscriptionByClient(clientUserId)
                .map(subscription -> {
                    syncMonthlyUsage(subscription);
                    reconcileTournamentsUsedThisMonth(subscription);
                    return UserPlanResponse.from(subscription);
                })
                .orElse(null);
    }

    @Transactional
    public UserSubscription syncMonthlyUsage(UserSubscription subscription) {
        if (subscription == null) {
            return null;
        }

        String currentMonth = java.time.YearMonth.now().toString();
        String storedMonth = subscription.getTournamentsUsageMonth();

        if (storedMonth == null || storedMonth.isBlank()) {
            subscription.setTournamentsUsageMonth(currentMonth);
            return userSubscriptionRepository.save(subscription);
        }

        if (!currentMonth.equals(storedMonth)) {
            subscription.setTournamentsUsageMonth(currentMonth);
            subscription.setTournamentsUsedThisMonth(0);
            subscription.setTournamentsUsageBaseline(0);
            return userSubscriptionRepository.save(subscription);
        }

        return subscription;
    }

    @Transactional
    public void reconcileTournamentsUsedThisMonth(UserSubscription subscription) {
        if (subscription == null || subscription.getClient() == null) {
            return;
        }

        Integer clientUserId = subscription.getClient().getUserId();
        if (clientUserId == null) {
            return;
        }

        java.time.YearMonth current = java.time.YearMonth.now();
        LocalDateTime monthStart = current.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = current.plusMonths(1).atDay(1).atStartOfDay();

        long actualCount = tournamentRepository.countByClientUserIdInCurrentMonth(
                clientUserId, monthStart, monthEnd);
        int baseline = subscription.getTournamentsUsageBaseline() != null
                ? subscription.getTournamentsUsageBaseline()
                : 0;
        int effectiveCount = Math.max(0, (int) actualCount - baseline);
        int storedCount = subscription.getTournamentsUsedThisMonth() != null
                ? subscription.getTournamentsUsedThisMonth()
                : 0;

        if (storedCount != effectiveCount) {
            subscription.setTournamentsUsedThisMonth(effectiveCount);
            userSubscriptionRepository.save(subscription);
        }
    }

    @Transactional
    public UserPlanResponse adminResetSubscriptionUsage(Integer clientUserId) {
        if (clientUserId == null) {
            throw BusinessException.badRequest("Cliente inválido");
        }

        processDueSubscriptionChanges(clientUserId);

        UserSubscription subscription = findActiveSubscriptionByClient(clientUserId)
                .orElseThrow(() -> BusinessException.notFound("Nenhum plano ativo para este cliente"));

        java.time.YearMonth current = java.time.YearMonth.now();
        LocalDateTime monthStart = current.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = current.plusMonths(1).atDay(1).atStartOfDay();

        long actualCount = tournamentRepository.countByClientUserIdInCurrentMonth(
                clientUserId, monthStart, monthEnd);

        subscription.setTournamentsUsageBaseline((int) actualCount);
        subscription.setTournamentsUsedThisMonth(0);
        subscription.setTournamentsUsageMonth(current.toString());
        subscription.setCancelAtPeriodEnd(false);
        subscription.setPendingPlan(null);
        userSubscriptionRepository.save(subscription);

        auditService.recordStaffMessage("RESET", "subscription", clientUserId.longValue(),
                "Uso mensal do plano resetado pelo staff (baseline=" + actualCount + ")");

        return UserPlanResponse.from(subscription);
    }

    public Optional<UserSubscription> findActiveSubscriptionByClient(Integer clientUserId) {
        return userSubscriptionRepository.findActiveByClientUserId(clientUserId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Page<AdminSubscriptionResponse> listActiveSubscriptions(String search, Pageable pageable) {
        String term = search != null ? search.trim() : "";
        if (term.isEmpty()) {
            term = null;
        }

        return userSubscriptionRepository
                .findActiveSubscriptions(LocalDateTime.now(), term, pageable)
                .map(AdminSubscriptionResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AdminSubscriptionResponse> listActiveSubscriptionsByPlan(Long planId, String search, Pageable pageable) {
        if (planId == null) {
            throw BusinessException.badRequest("Plano inválido");
        }

        if (!planRepository.existsById(planId)) {
            throw BusinessException.notFound("Plano não encontrado");
        }

        String term = search != null ? search.trim() : "";
        if (term.isEmpty()) {
            term = null;
        }

        return userSubscriptionRepository
                .findActiveSubscriptionsByPlan(LocalDateTime.now(), planId, term, pageable)
                .map(AdminSubscriptionResponse::from);
    }

    public AdminSubscriptionResponse getActiveSubscriptionForAdmin(Integer clientUserId) {
        if (clientUserId == null) {
            throw BusinessException.badRequest("Cliente inválido");
        }

        processDueSubscriptionChanges(clientUserId);

        UserSubscription subscription = findActiveSubscriptionByClient(clientUserId)
                .orElseThrow(() -> BusinessException.notFound("Nenhum plano ativo para este cliente"));

        return AdminSubscriptionResponse.from(subscription);
    }

    @Transactional
    public UserPlanResponse adminAssignPlan(Integer clientUserId, Long planId, Integer billingPeriodMonths) {
        if (clientUserId == null) {
            throw BusinessException.badRequest("Cliente inválido");
        }

        int periodMonths = normalizeBillingPeriodMonths(billingPeriodMonths);
        Client client = clientRepository.findById(clientUserId)
                .orElseThrow(() -> BusinessException.notFound("Cliente não encontrado"));
        Plan plan = loadPlanForAdmin(planId);

        processDueSubscriptionChanges(clientUserId);
        deactivateActiveSubscriptions(clientUserId);

        UserPlanResponse result = UserPlanResponse.from(createSubscription(client, plan, periodMonths));
        auditService.recordStaffMessage("ASSIGN", "subscription", clientUserId.longValue(),
                "Plano " + plan.getName() + " atribuído por " + periodMonths + " mês(es)");
        return result;
    }

    @Transactional
    public void adminRemovePlan(Integer clientUserId) {
        if (clientUserId == null) {
            throw BusinessException.badRequest("Cliente inválido");
        }

        processDueSubscriptionChanges(clientUserId);
        deactivateActiveSubscriptions(clientUserId);
        auditService.recordStaffMessage("REMOVE", "subscription", clientUserId.longValue(),
                "Plano removido do cliente");
    }

    @Transactional
    public UserPlanResponse subscribe(Integer contactId, Long planId, Integer billingPeriodMonths) {
        int periodMonths = normalizeBillingPeriodMonths(billingPeriodMonths);
        Plan plan = loadSubscribablePlan(planId);
        Contact contact = requirePrimaryContact(contactId);
        Integer clientUserId = contact.getUserid();
        Client client = clientRepository.findById(clientUserId)
                .orElseThrow(() -> BusinessException.notFound("Cliente não encontrado"));

        processDueSubscriptionChanges(clientUserId);

        Optional<UserSubscription> existingOpt = findActiveSubscriptionByClient(clientUserId);
        if (existingOpt.isEmpty()) {
            return UserPlanResponse.from(createSubscription(client, plan, periodMonths));
        }

        UserSubscription existing = existingOpt.get();
        Plan currentPlan = existing.getPlan();

        if (currentPlan.getId().equals(planId)) {
            return UserPlanResponse.from(extendSubscription(existing, periodMonths));
        }

        if (isUpgrade(currentPlan, plan)) {
            existing.setActive(false);
            userSubscriptionRepository.save(existing);
            return UserPlanResponse.from(createSubscription(client, plan, periodMonths));
        }

        scheduleDowngrade(existing, plan);
        return UserPlanResponse.from(existing);
    }

    @Transactional
    public UserPlanResponse subscribeWithCredits(Integer contactId, Long planId, Integer billingPeriodMonths) {
        int periodMonths = normalizeBillingPeriodMonths(billingPeriodMonths);
        Plan plan = loadSubscribablePlan(planId);
        Contact contact = requirePrimaryContact(contactId);
        Integer clientUserId = contact.getUserid();

        processDueSubscriptionChanges(clientUserId);

        Optional<UserSubscription> existingOpt = findActiveSubscriptionByClient(clientUserId);
        boolean schedulesDowngrade = false;

        if (existingOpt.isPresent()) {
            UserSubscription existing = existingOpt.get();
            Plan currentPlan = existing.getPlan();
            if (!currentPlan.getId().equals(planId) && !isUpgrade(currentPlan, plan)) {
                schedulesDowngrade = true;
            }
        }

        if (!schedulesDowngrade && !isFreePlan(plan)) {
            BigDecimal amount = calculateBillingAmount(plan, periodMonths);
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                walletService.requireAvailableBalance(clientUserId, amount);
                walletService.withdrawForContact(
                        contact,
                        amount,
                        "Assinatura plano " + plan.getName() + " (" + periodMonths + " mês(es))");
            }
        }

        return subscribe(contactId, planId, billingPeriodMonths);
    }

    public BigDecimal calculateBillingAmount(Plan plan, Integer billingPeriodMonths) {
        if (plan == null || isFreePlan(plan)) {
            return BigDecimal.ZERO;
        }

        int periodMonths = normalizeBillingPeriodMonths(billingPeriodMonths);
        BigDecimal monthly = plan.getMonthlyPrice() != null ? plan.getMonthlyPrice() : BigDecimal.ZERO;
        int discount = switch (periodMonths) {
            case 6 -> 20;
            case 12 -> 30;
            default -> 0;
        };

        BigDecimal gross = monthly.multiply(BigDecimal.valueOf(periodMonths));
        return gross.multiply(BigDecimal.valueOf(100 - discount))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Transactional
    public UserPlanResponse scheduleCancel(Integer contactId) {
        Contact contact = requirePrimaryContact(contactId);

        UserSubscription subscription = findActiveSubscriptionByClient(contact.getUserid())
                .orElseThrow(() -> BusinessException.notFound("Nenhum plano ativo encontrado para esta conta"));

        if (isFreePlan(subscription.getPlan())) {
            throw BusinessException.badRequest("O plano Free não pode ser cancelado");
        }

        if (Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd())) {
            throw BusinessException.badRequest("O cancelamento já está agendado para o fim do período");
        }

        subscription.setCancelAtPeriodEnd(true);
        subscription.setPendingPlan(null);
        userSubscriptionRepository.save(subscription);

        return UserPlanResponse.from(subscription);
    }

    @Transactional
    public void processDueSubscriptionChanges(Integer clientUserId) {
        if (clientUserId == null) {
            return;
        }

        for (UserSubscription subscription : userSubscriptionRepository.findDueActiveByClientUserId(
                clientUserId, LocalDateTime.now())) {
            subscription.setActive(false);
            userSubscriptionRepository.save(subscription);

            if (Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd())) {
                continue;
            }

            if (subscription.getPendingPlan() != null) {
                createSubscription(subscription.getClient(), subscription.getPendingPlan());
            }
        }
    }

    private UserSubscription createSubscription(Client client, Plan plan) {
        return createSubscription(client, plan, 1);
    }

    private UserSubscription createSubscription(Client client, Plan plan, int billingPeriodMonths) {
        LocalDateTime now = LocalDateTime.now();
        UserSubscription subscription = UserSubscription.builder()
                .client(client)
                .plan(plan)
                .startsAt(now)
                .expiresAt(now.plusMonths(billingPeriodMonths))
                .billingPeriodMonths(billingPeriodMonths)
                .tournamentsUsedThisMonth(0)
                .tournamentsUsageBaseline(0)
                .tournamentsUsageMonth(java.time.YearMonth.now().toString())
                .active(true)
                .cancelAtPeriodEnd(false)
                .build();

        return userSubscriptionRepository.save(subscription);
    }

    private UserSubscription extendSubscription(UserSubscription existing, int billingPeriodMonths) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime base = existing.getExpiresAt() != null && existing.getExpiresAt().isAfter(now)
                ? existing.getExpiresAt()
                : now;

        existing.setExpiresAt(base.plusMonths(billingPeriodMonths));
        existing.setBillingPeriodMonths(billingPeriodMonths);
        existing.setCancelAtPeriodEnd(false);
        existing.setPendingPlan(null);
        existing.setActive(true);

        return userSubscriptionRepository.save(existing);
    }

    private int normalizeBillingPeriodMonths(Integer billingPeriodMonths) {
        if (billingPeriodMonths == null) {
            return 1;
        }

        return switch (billingPeriodMonths) {
            case 6, 12 -> billingPeriodMonths;
            default -> 1;
        };
    }

    private void scheduleDowngrade(UserSubscription existing, Plan targetPlan) {
        if (Boolean.TRUE.equals(existing.getCancelAtPeriodEnd())) {
            existing.setCancelAtPeriodEnd(false);
        }

        existing.setPendingPlan(targetPlan);
        userSubscriptionRepository.save(existing);
    }

    private Plan loadSubscribablePlan(Long planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> BusinessException.notFound("Plano não encontrado"));

        if (!Boolean.TRUE.equals(plan.getActive()) || Boolean.TRUE.equals(plan.getHidden())) {
            throw BusinessException.badRequest("Plano indisponível para contratação");
        }

        return plan;
    }

    private Plan loadPlanForAdmin(Long planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> BusinessException.notFound("Plano não encontrado"));

        if (!Boolean.TRUE.equals(plan.getActive())) {
            throw BusinessException.badRequest("Plano inativo");
        }

        return plan;
    }

    private void deactivateActiveSubscriptions(Integer clientUserId) {
        List<UserSubscription> subscriptions = userSubscriptionRepository.findAllActiveByClientUserId(clientUserId);
        for (UserSubscription subscription : subscriptions) {
            subscription.setActive(false);
            subscription.setCancelAtPeriodEnd(false);
            subscription.setPendingPlan(null);
            userSubscriptionRepository.save(subscription);
        }
    }

    private Contact requirePrimaryContact(Integer contactId) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> BusinessException.notFound("Contato não encontrado"));

        if (contact.getIsPrimary() == null || contact.getIsPrimary() != 1) {
            throw BusinessException.forbidden(
                    "Somente o contato principal pode gerenciar planos. Entre em contato com o administrador da conta.");
        }

        return contact;
    }

    private boolean isUpgrade(Plan currentPlan, Plan targetPlan) {
        BigDecimal currentPrice = currentPlan.getMonthlyPrice() != null ? currentPlan.getMonthlyPrice() : BigDecimal.ZERO;
        BigDecimal targetPrice = targetPlan.getMonthlyPrice() != null ? targetPlan.getMonthlyPrice() : BigDecimal.ZERO;
        return targetPrice.compareTo(currentPrice) > 0;
    }

    private boolean isFreePlan(Plan plan) {
        if (plan.getMonthlyPrice() != null && plan.getMonthlyPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }

        return plan.getName() != null && plan.getName().trim().equalsIgnoreCase("Free");
    }
}
