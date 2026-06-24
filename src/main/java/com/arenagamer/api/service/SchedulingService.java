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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SchedulingService {

    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final TournamentAccessService tournamentAccessService;
    private final AuditService auditService;
    private final AvailabilityService availabilityService;

    @Transactional
    public List<Match> scheduleMatches(String slug) {
        Tournament tournament = tournamentRepository.findBySlug(slug)
                .orElseThrow(() -> BusinessException.notFound("Torneio não encontrado"));

        List<Match> matches = matchRepository.findByTournamentId(tournament.getId());
        LocalDateTime baseDate = tournament.getStartDate() != null
                ? tournament.getStartDate()
                : LocalDateTime.now().plusDays(1);

        int matchesPerDay = 4;
        int dayOffset = 0;
        int matchCount = 0;

        for (Match match : matches) {
            if (match.getStatus() == MatchStatus.WALKOVER || match.getScheduledAt() != null) {
                continue;
            }

            if (match.getHomeParticipant() == null || match.getAwayParticipant() == null) {
                continue;
            }

            TimeWindow bestWindow = findBestWindow(match);
            LocalDateTime scheduledTime = findScheduleTime(baseDate, dayOffset, bestWindow, match);

            match.setScheduledAt(scheduledTime);
            match.setTimeWindow(bestWindow);
            matchRepository.save(match);

            matchCount++;
            if (matchCount >= matchesPerDay) {
                matchCount = 0;
                dayOffset++;
            }
        }

        UserPrincipal.tryCurrent()
                .filter(AuthenticatedUser::isStaff)
                .ifPresent(auth -> auditService.recordStaffMessage(auth, "SCHEDULE", "tournament",
                        tournament.getId(), "Partidas agendadas: " + slug));

        return matchRepository.findByTournamentId(tournament.getId());
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

    private TimeWindow findBestWindow(Match match) {
        Set<TimeWindow> homeWindows = getParticipantWindows(match.getHomeParticipant());
        Set<TimeWindow> awayWindows = getParticipantWindows(match.getAwayParticipant());

        if (homeWindows.isEmpty() && awayWindows.isEmpty()) {
            return TimeWindow.EVENING;
        }

        Set<TimeWindow> intersection = new HashSet<>(homeWindows);
        intersection.retainAll(awayWindows);

        if (!intersection.isEmpty()) {
            for (TimeWindow preferred : List.of(TimeWindow.EVENING, TimeWindow.AFTERNOON, TimeWindow.MORNING, TimeWindow.NIGHT)) {
                if (intersection.contains(preferred)) return preferred;
            }
        }

        Set<TimeWindow> union = new HashSet<>(homeWindows);
        union.addAll(awayWindows);
        return union.isEmpty() ? TimeWindow.EVENING : union.iterator().next();
    }

    private Set<TimeWindow> getParticipantWindows(TournamentParticipant participant) {
        if (participant == null || participant.getAvailabilityProfile() == null) {
            return EnumSet.allOf(TimeWindow.class);
        }
        return availabilityService.resolveTimeWindows(participant.getAvailabilityProfile());
    }

    private LocalDateTime findScheduleTime(
            LocalDateTime baseDate, int dayOffset, TimeWindow window, Match match) {
        for (int offset = dayOffset; offset < dayOffset + 14; offset++) {
            LocalDateTime candidate = calculateScheduleTime(baseDate, offset, window);
            DayOfWeek day = candidate.getDayOfWeek();
            LocalTime time = candidate.toLocalTime();
            if (isParticipantAvailable(match.getHomeParticipant(), day, time)
                    && isParticipantAvailable(match.getAwayParticipant(), day, time)) {
                return candidate;
            }
        }
        return calculateScheduleTime(baseDate, dayOffset, window);
    }

    private boolean isParticipantAvailable(TournamentParticipant participant, DayOfWeek day, LocalTime time) {
        if (participant == null || participant.getAvailabilityProfile() == null) {
            return true;
        }
        return availabilityService.isAvailableAt(participant.getAvailabilityProfile(), day, time);
    }

    private LocalDateTime calculateScheduleTime(LocalDateTime baseDate, int dayOffset, TimeWindow window) {
        LocalTime time = switch (window) {
            case MORNING -> LocalTime.of(9, 0);
            case AFTERNOON -> LocalTime.of(14, 0);
            case EVENING -> LocalTime.of(20, 0);
            case NIGHT -> LocalTime.of(1, 0);
        };
        return baseDate.plusDays(dayOffset).with(time);
    }

    private TimeWindow timeWindowFromHour(int hour) {
        if (hour >= 6 && hour < 12) return TimeWindow.MORNING;
        if (hour >= 12 && hour < 18) return TimeWindow.AFTERNOON;
        if (hour >= 18) return TimeWindow.EVENING;
        return TimeWindow.NIGHT;
    }
}
