package com.arenagamer.api.service;

import com.arenagamer.api.entity.*;
import com.arenagamer.api.entity.enums.MatchStatus;
import com.arenagamer.api.entity.enums.TimeWindow;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.MatchRepository;
import com.arenagamer.api.repository.TournamentRepository;
import com.arenagamer.api.security.AuthenticatedUser;
import com.arenagamer.api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SchedulingService {

    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final TournamentAccessService tournamentAccessService;
    private final AuditService auditService;
    private final AvailabilityService availabilityService;

    private static final int DEFAULT_MATCHES_PER_DAY = 4;
    private static final int DEFAULT_HORIZON_DAYS = 30;
    private static final List<TimeWindow> WINDOW_PREFERENCE =
            List.of(TimeWindow.EVENING, TimeWindow.AFTERNOON, TimeWindow.MORNING, TimeWindow.NIGHT);

    @Transactional
    public List<Match> scheduleMatches(String slug) {
        Tournament tournament = tournamentRepository.findBySlug(slug)
                .orElseThrow(() -> BusinessException.notFound("Torneio não encontrado"));

        List<Match> matches = matchRepository.findByTournamentId(tournament.getId());

        LocalDateTime windowStart = tournament.getStartDate() != null
                ? tournament.getStartDate()
                : LocalDateTime.now().plusDays(1);

        boolean hasDeadline = tournament.getExpectedEndDate() != null
                && tournament.getExpectedEndDate().isAfter(windowStart);
        LocalDateTime windowEnd = hasDeadline
                ? tournament.getExpectedEndDate()
                : windowStart.plusDays(DEFAULT_HORIZON_DAYS);

        List<Match> schedulable = matches.stream()
                .filter(m -> m.getStatus() != MatchStatus.WALKOVER && m.getScheduledAt() == null)
                .filter(m -> m.getHomeParticipant() != null && m.getAwayParticipant() != null)
                .toList();

        long windowDays = ChronoUnit.DAYS.between(windowStart.toLocalDate(), windowEnd.toLocalDate()) + 1;
        int matchesPerDay = hasDeadline
                ? (int) Math.max(1, Math.ceil((double) schedulable.size() / Math.max(1, windowDays)))
                : DEFAULT_MATCHES_PER_DAY;

        Map<LocalDate, Integer> perDayCount = new HashMap<>();
        Map<Long, Set<LocalDateTime>> participantBookings = new HashMap<>();

        for (Match match : schedulable) {
            AvailabilityProfile home = profileOf(match.getHomeParticipant());
            AvailabilityProfile away = profileOf(match.getAwayParticipant());

            ScheduledSlot slot = findCommonSlot(
                    home, away, windowStart, windowEnd, matchesPerDay, perDayCount, participantBookings, match);
            if (slot == null) {
                slot = findFallbackSlot(
                        home, away, windowStart, windowEnd, matchesPerDay, perDayCount, participantBookings, match);
            }

            match.setScheduledAt(slot.dateTime());
            match.setTimeWindow(slot.window());
            matchRepository.save(match);

            perDayCount.merge(slot.dateTime().toLocalDate(), 1, Integer::sum);
            bookParticipant(participantBookings, match.getHomeParticipant(), slot.dateTime());
            bookParticipant(participantBookings, match.getAwayParticipant(), slot.dateTime());
        }

        UserPrincipal.tryCurrent()
                .filter(AuthenticatedUser::isStaff)
                .ifPresent(auth -> auditService.recordStaffMessage(auth, "SCHEDULE", "tournament",
                        tournament.getId(), "Partidas agendadas: " + slug));

        return matchRepository.findByTournamentIdWithParticipants(tournament.getId());
    }

    public void validateReschedulePermission(Long matchId, AuthenticatedUser auth) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> BusinessException.notFound("Partida não encontrada"));
        Tournament tournament = match.getRound().getTournament();
        tournamentAccessService.validateCanManage(tournament, auth);
    }

    @Transactional
    public Match reschedule(Long matchId, LocalDateTime newTime) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> BusinessException.notFound("Partida não encontrada"));

        if (match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.CANCELLED) {
            throw BusinessException.badRequest("Partida já finalizada ou cancelada");
        }

        match.setScheduledAt(newTime);
        match.setTimeWindow(timeWindowFromHour(newTime.getHour()));
        match.setStatus(MatchStatus.RESCHEDULED);

        Match saved = matchRepository.save(match);
        Tournament tournament = saved.getRound().getTournament();

        UserPrincipal.tryCurrent()
                .filter(AuthenticatedUser::isStaff)
                .ifPresent(auth -> auditService.recordStaffMessage(auth, "RESCHEDULE", "match",
                        matchId, "Partida reagendada no torneio " + tournament.getSlug()));

        return saved;
    }

    private record ScheduledSlot(LocalDateTime dateTime, TimeWindow window) {
    }

    /**
     * Procura a primeira data dentro da janela cujo dia da semana tenha um horário em comum
     * entre os dois participantes, respeitando o limite por dia e evitando reservas duplicadas.
     * Retorna {@code null} quando não há horário semanal comum (ou ambos são "sempre disponíveis").
     */
    private ScheduledSlot findCommonSlot(
            AvailabilityProfile home, AvailabilityProfile away,
            LocalDateTime windowStart, LocalDateTime windowEnd, int matchesPerDay,
            Map<LocalDate, Integer> perDayCount, Map<Long, Set<LocalDateTime>> bookings, Match match) {

        List<AvailabilityService.CommonSlot> common = availabilityService.findCommonWeeklySlots(home, away);
        if (common.isEmpty()) {
            return null;
        }

        LocalDate cursor = windowStart.toLocalDate();
        LocalDate lastDay = windowEnd.toLocalDate();
        while (!cursor.isAfter(lastDay)) {
            if (perDayCount.getOrDefault(cursor, 0) < matchesPerDay) {
                DayOfWeek day = cursor.getDayOfWeek();
                LocalTime earliest = common.stream()
                        .filter(slot -> slot.dayOfWeek() == day)
                        .map(AvailabilityService.CommonSlot::startTime)
                        .min(LocalTime::compareTo)
                        .orElse(null);
                if (earliest != null) {
                    LocalDateTime candidate = cursor.atTime(earliest);
                    if (!candidate.isBefore(windowStart) && !candidate.isAfter(windowEnd)
                            && isFree(bookings, match, candidate)) {
                        return new ScheduledSlot(candidate, timeWindowFromHour(candidate.getHour()));
                    }
                }
            }
            cursor = cursor.plusDays(1);
        }
        return null;
    }

    /**
     * Sem horário semanal comum: agenda no dia mais próximo dentro da janela usando um
     * {@link TimeWindow} comum (ou {@code EVENING} por padrão). Como último recurso, encaixa
     * no último dia sem ultrapassar a data final, mesmo que estoure o limite diário.
     */
    private ScheduledSlot findFallbackSlot(
            AvailabilityProfile home, AvailabilityProfile away,
            LocalDateTime windowStart, LocalDateTime windowEnd, int matchesPerDay,
            Map<LocalDate, Integer> perDayCount, Map<Long, Set<LocalDateTime>> bookings, Match match) {

        TimeWindow window = pickCommonWindow(home, away);
        LocalDate cursor = windowStart.toLocalDate();
        LocalDate lastDay = windowEnd.toLocalDate();

        while (!cursor.isAfter(lastDay)) {
            if (perDayCount.getOrDefault(cursor, 0) < matchesPerDay) {
                LocalDateTime candidate = clampToWindow(cursor.atTime(timeForWindow(window)), windowStart, windowEnd);
                if (isFree(bookings, match, candidate)) {
                    return new ScheduledSlot(candidate, timeWindowFromHour(candidate.getHour()));
                }
            }
            cursor = cursor.plusDays(1);
        }

        LocalDateTime fallback = clampToWindow(lastDay.atTime(timeForWindow(window)), windowStart, windowEnd);
        return new ScheduledSlot(fallback, timeWindowFromHour(fallback.getHour()));
    }

    private TimeWindow pickCommonWindow(AvailabilityProfile home, AvailabilityProfile away) {
        Set<TimeWindow> common = availabilityService.commonTimeWindows(home, away);
        for (TimeWindow preferred : WINDOW_PREFERENCE) {
            if (common.contains(preferred)) {
                return preferred;
            }
        }
        return TimeWindow.EVENING;
    }

    private boolean isFree(Map<Long, Set<LocalDateTime>> bookings, Match match, LocalDateTime candidate) {
        return isParticipantFree(bookings, match.getHomeParticipant(), candidate)
                && isParticipantFree(bookings, match.getAwayParticipant(), candidate);
    }

    private boolean isParticipantFree(
            Map<Long, Set<LocalDateTime>> bookings, TournamentParticipant participant, LocalDateTime candidate) {
        if (participant == null || participant.getId() == null) {
            return true;
        }
        return !bookings.getOrDefault(participant.getId(), Set.of()).contains(candidate);
    }

    private void bookParticipant(
            Map<Long, Set<LocalDateTime>> bookings, TournamentParticipant participant, LocalDateTime dateTime) {
        if (participant == null || participant.getId() == null) {
            return;
        }
        bookings.computeIfAbsent(participant.getId(), k -> new HashSet<>()).add(dateTime);
    }

    private AvailabilityProfile profileOf(TournamentParticipant participant) {
        return participant == null ? null : participant.getAvailabilityProfile();
    }

    private LocalDateTime clampToWindow(LocalDateTime candidate, LocalDateTime windowStart, LocalDateTime windowEnd) {
        if (candidate.isBefore(windowStart)) {
            return windowStart;
        }
        if (candidate.isAfter(windowEnd)) {
            return windowEnd;
        }
        return candidate;
    }

    private LocalTime timeForWindow(TimeWindow window) {
        return switch (window) {
            case MORNING -> LocalTime.of(9, 0);
            case AFTERNOON -> LocalTime.of(14, 0);
            case EVENING -> LocalTime.of(20, 0);
            case NIGHT -> LocalTime.of(1, 0);
        };
    }

    private TimeWindow timeWindowFromHour(int hour) {
        if (hour >= 6 && hour < 12) return TimeWindow.MORNING;
        if (hour >= 12 && hour < 18) return TimeWindow.AFTERNOON;
        if (hour >= 18) return TimeWindow.EVENING;
        return TimeWindow.NIGHT;
    }
}
