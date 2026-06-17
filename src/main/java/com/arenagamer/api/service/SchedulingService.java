package com.arenagamer.api.service;

import com.arenagamer.api.entity.*;
import com.arenagamer.api.entity.enums.MatchStatus;
import com.arenagamer.api.entity.enums.TimeWindow;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.MatchRepository;
import com.arenagamer.api.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SchedulingService {

    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;

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

            // Only schedule if both participants are set
            if (match.getHomeParticipant() == null || match.getAwayParticipant() == null) {
                continue;
            }

            TimeWindow bestWindow = findBestWindow(match);
            LocalDateTime scheduledTime = calculateScheduleTime(baseDate, dayOffset, bestWindow);

            match.setScheduledAt(scheduledTime);
            match.setTimeWindow(bestWindow);
            matchRepository.save(match);

            matchCount++;
            if (matchCount >= matchesPerDay) {
                matchCount = 0;
                dayOffset++;
            }
        }

        return matchRepository.findByTournamentId(tournament.getId());
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

        return matchRepository.save(match);
    }

    private TimeWindow findBestWindow(Match match) {
        Set<TimeWindow> homeWindows = getParticipantWindows(match.getHomeParticipant());
        Set<TimeWindow> awayWindows = getParticipantWindows(match.getAwayParticipant());

        if (homeWindows.isEmpty() && awayWindows.isEmpty()) {
            return TimeWindow.EVENING; // default
        }

        // Find intersection
        Set<TimeWindow> intersection = new HashSet<>(homeWindows);
        intersection.retainAll(awayWindows);

        if (!intersection.isEmpty()) {
            // Prefer EVENING > AFTERNOON > MORNING > NIGHT
            for (TimeWindow preferred : List.of(TimeWindow.EVENING, TimeWindow.AFTERNOON, TimeWindow.MORNING, TimeWindow.NIGHT)) {
                if (intersection.contains(preferred)) return preferred;
            }
        }

        // Fallback: use any available window
        Set<TimeWindow> union = new HashSet<>(homeWindows);
        union.addAll(awayWindows);
        return union.isEmpty() ? TimeWindow.EVENING : union.iterator().next();
    }

    private Set<TimeWindow> getParticipantWindows(TournamentParticipant participant) {
        if (participant == null || participant.getAvailabilityProfile() == null) {
            return EnumSet.allOf(TimeWindow.class);
        }
        Set<TimeWindow> windows = participant.getAvailabilityProfile().getWindows();
        return windows.isEmpty() ? EnumSet.allOf(TimeWindow.class) : windows;
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
