package com.arenagamer.api.service;

import com.arenagamer.api.dto.request.CreateTournamentRequest;
import com.arenagamer.api.entity.Plan;
import com.arenagamer.api.entity.UserSubscription;
import com.arenagamer.api.entity.enums.PrizeFunding;
import com.arenagamer.api.entity.enums.PrizeType;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PlanEntitlementService {

    private static final BigDecimal ENTRY_FEE_MIN_CREATION_COST = new BigDecimal("5");

    private final SubscriptionService subscriptionService;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final TournamentPricingService tournamentPricingService;
    private final WalletService walletService;

    public record TournamentCreationEntitlement(
            UserSubscription subscription,
            Plan plan,
            BigDecimal creditCost,
            boolean freeTournamentSlot
    ) {}

    @Transactional
    public TournamentCreationEntitlement prepareTournamentCreation(Integer clientUserId, CreateTournamentRequest request) {
        if (clientUserId == null) {
            throw BusinessException.badRequest("Cliente do torneio não informado");
        }

        UserSubscription subscription = subscriptionService.findActiveSubscriptionByClient(clientUserId)
                .orElseThrow(() -> BusinessException.forbidden("Plano ativo necessário para criar torneios"));

        subscription = subscriptionService.syncMonthlyUsage(subscription);
        Plan plan = subscription.getPlan();

        int participantsLimit = request.getParticipantsLimit() != null ? request.getParticipantsLimit() : 0;
        int used = safeInt(subscription.getTournamentsUsedThisMonth());

        BigDecimal entryFee = request.getEntryFeeCredits() != null ? request.getEntryFeeCredits() : BigDecimal.ZERO;

        int freeTournamentsPerMonth = safeInt(plan.getFreeTournamentsPerMonth());
        boolean tournamentLimitReached = freeTournamentsPerMonth > 0 && used >= freeTournamentsPerMonth;
        boolean freeSlot = freeTournamentsPerMonth > 0 && used < freeTournamentsPerMonth;
        BigDecimal creditCost = calculateCreationCost(plan, participantsLimit, freeSlot, !tournamentLimitReached);
        creditCost = creditCost.add(resolvePrizePoolCreationCost(request));

        PrizeFunding funding = request.getPrizeFunding() != null ? request.getPrizeFunding() : PrizeFunding.FIXED;
        boolean requiresEntryFeeBenefit = funding == PrizeFunding.ENTRY_FEES
                || entryFee.compareTo(BigDecimal.ZERO) > 0;
        if (requiresEntryFeeBenefit
                && !Boolean.TRUE.equals(plan.getAllowsEntryFee())
                && creditCost.compareTo(ENTRY_FEE_MIN_CREATION_COST) < 0) {
            throw BusinessException.badRequest(
                    "Taxa de inscrição só é permitida após gastar 5 créditos na criação além da isenção do plano ou se seu plano inclui esse benefício");
        }

        if (creditCost.compareTo(BigDecimal.ZERO) > 0) {
            walletService.requireAvailableBalance(clientUserId, creditCost);
        }

        return new TournamentCreationEntitlement(subscription, plan, creditCost, freeSlot);
    }

    @Transactional
    public void recordTournamentCreated(UserSubscription subscription) {
        if (subscription == null || subscription.getId() == null) {
            return;
        }

        UserSubscription managed = userSubscriptionRepository.findById(subscription.getId())
                .orElse(subscription);

        managed = subscriptionService.syncMonthlyUsage(managed);
        managed.setTournamentsUsedThisMonth(safeInt(managed.getTournamentsUsedThisMonth()) + 1);
        userSubscriptionRepository.save(managed);
    }

    public BigDecimal calculateCreationCost(Plan plan, int participantsLimit) {
        return calculateCreationCost(plan, participantsLimit, false, true);
    }

    public BigDecimal calculateCreationCost(Plan plan, int participantsLimit, boolean waiveBasePrice) {
        return calculateCreationCost(plan, participantsLimit, waiveBasePrice, true);
    }

    public BigDecimal calculateCreationCost(Plan plan, int participantsLimit, boolean waiveBasePrice, boolean applyPlanParticipantBenefits) {
        var settings = tournamentPricingService.getSettings();
        int globalIncluded = Math.max(2, safeInt(settings.getIncludedParticipants()));
        int planIncluded = safeInt(plan.getFreeMaxParticipants());
        int includedForBilling = applyPlanParticipantBenefits
                ? (planIncluded > 0 ? planIncluded : globalIncluded)
                : globalIncluded;

        BigDecimal basePrice = settings.getBaseTournamentPrice() != null
                ? settings.getBaseTournamentPrice()
                : BigDecimal.ZERO;
        BigDecimal cost = waiveBasePrice ? BigDecimal.ZERO : basePrice;

        if (participantsLimit > includedForBilling) {
            BigDecimal extraPrice = settings.getExtraParticipantPrice() != null
                    ? settings.getExtraParticipantPrice()
                    : BigDecimal.ZERO;
            cost = cost.add(extraPrice.multiply(BigDecimal.valueOf(participantsLimit - includedForBilling)));
        }

        return cost.max(BigDecimal.ZERO);
    }

    public int resolveMaxTournamentsPerMonth(Plan plan) {
        if (plan == null) {
            return 0;
        }

        int max = safeInt(plan.getMaxTournamentsPerMonth());
        if (max > 0) {
            return max;
        }

        return safeInt(plan.getFreeTournamentsPerMonth());
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private BigDecimal resolvePrizePoolCreationCost(CreateTournamentRequest request) {
        PrizeType prizeType = request.getPrizeType() != null ? request.getPrizeType() : PrizeType.MANUAL;
        PrizeFunding funding = request.getPrizeFunding() != null ? request.getPrizeFunding() : PrizeFunding.FIXED;

        if (prizeType != PrizeType.AUTOMATIC || funding != PrizeFunding.FIXED) {
            return BigDecimal.ZERO;
        }

        BigDecimal pool = request.getPrizePool() != null ? request.getPrizePool() : BigDecimal.ZERO;
        return pool.max(BigDecimal.ZERO);
    }
}
