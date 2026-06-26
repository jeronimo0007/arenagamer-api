package com.arenagamer.api.service;

import com.arenagamer.api.entity.AvailabilityProfile;
import com.arenagamer.api.entity.Match;
import com.arenagamer.api.entity.Tournament;
import com.arenagamer.api.entity.TournamentParticipant;
import com.arenagamer.api.entity.WeeklyAvailabilitySlot;
import com.arenagamer.api.entity.enums.MatchStatus;
import com.arenagamer.api.entity.enums.TimeWindow;
import com.arenagamer.api.repository.MatchRepository;
import com.arenagamer.api.repository.TournamentRepository;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchedulingServiceTest {

    private static final LocalDateTime START = LocalDateTime.of(2025, 1, 6, 0, 0); // segunda-feira
    private static final LocalDateTime END = START.plusDays(7);

    private final TournamentRepository tournamentRepository = mock(TournamentRepository.class);
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final SchedulingService schedulingService = new SchedulingService(
            tournamentRepository, matchRepository, null, null, new AvailabilityService(null));

    @Test
    void schedulesMatchAtCommonWeeklySlot() {
        AvailabilityProfile home = profile(weeklySlot(DayOfWeek.SATURDAY, "14:00", "18:00"));
        AvailabilityProfile away = profile(weeklySlot(DayOfWeek.SATURDAY, "16:00", "20:00"));
        Match match = match(participant(1L, home), participant(2L, away));

        Match scheduled = runSchedule(match);

        // Sábado dentro da janela, no início da interseção (16:00).
        assertThat(scheduled.getScheduledAt()).isEqualTo(LocalDateTime.of(2025, 1, 11, 16, 0));
        assertThat(scheduled.getTimeWindow()).isEqualTo(TimeWindow.AFTERNOON);
        assertThat(scheduled.getScheduledAt()).isBetween(START, END);
    }

    @Test
    void fallsBackToNearestDayWhenNoCommonWeeklySlot() {
        AvailabilityProfile home = profile(weeklySlot(DayOfWeek.MONDAY, "09:00", "11:00"));
        AvailabilityProfile away = profile(weeklySlot(DayOfWeek.TUESDAY, "09:00", "11:00"));
        Match match = match(participant(1L, home), participant(2L, away));

        Match scheduled = runSchedule(match);

        // Sem horário semanal comum: cai no dia mais próximo num TimeWindow comum (manhã).
        assertThat(scheduled.getScheduledAt()).isEqualTo(LocalDateTime.of(2025, 1, 6, 9, 0));
        assertThat(scheduled.getTimeWindow()).isEqualTo(TimeWindow.MORNING);
        assertThat(scheduled.getScheduledAt()).isBetween(START, END);
    }

    @Test
    void keepsAllMatchesWithinExpectedEndDate() {
        AvailabilityProfile home = profile(weeklySlot(DayOfWeek.SATURDAY, "14:00", "18:00"));
        AvailabilityProfile away = profile(weeklySlot(DayOfWeek.SATURDAY, "14:00", "18:00"));
        List<Match> matches = List.of(
                match(participant(1L, home), participant(2L, away)),
                match(participant(3L, home), participant(4L, away)),
                match(participant(5L, home), participant(6L, away)));

        runSchedule(matches);

        assertThat(matches).allSatisfy(m -> {
            assertThat(m.getScheduledAt()).isNotNull();
            assertThat(m.getScheduledAt()).isBetween(START, END);
        });
    }

    private Match runSchedule(Match... matches) {
        runSchedule(List.of(matches));
        return matches[0];
    }

    private void runSchedule(List<Match> matches) {
        Tournament tournament = Tournament.builder()
                .id(1L)
                .slug("torneio")
                .startDate(START)
                .expectedEndDate(END)
                .build();
        when(tournamentRepository.findBySlug("torneio")).thenReturn(Optional.of(tournament));
        when(matchRepository.findByTournamentId(anyLong())).thenReturn(matches);
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));

        schedulingService.scheduleMatches("torneio");
    }

    private static final AtomicLong MATCH_IDS = new AtomicLong(1);

    private static Match match(TournamentParticipant home, TournamentParticipant away) {
        return Match.builder()
                .id(MATCH_IDS.getAndIncrement())
                .homeParticipant(home)
                .awayParticipant(away)
                .status(MatchStatus.SCHEDULED)
                .build();
    }

    private static TournamentParticipant participant(Long id, AvailabilityProfile profile) {
        return TournamentParticipant.builder()
                .id(id)
                .availabilityProfile(profile)
                .build();
    }

    private static AvailabilityProfile profile(WeeklyAvailabilitySlot... slots) {
        AvailabilityProfile profile = new AvailabilityProfile();
        for (WeeklyAvailabilitySlot slot : slots) {
            slot.setAvailabilityProfile(profile);
            profile.getWeeklySlots().add(slot);
        }
        return profile;
    }

    private static WeeklyAvailabilitySlot weeklySlot(DayOfWeek day, String start, String end) {
        return WeeklyAvailabilitySlot.builder()
                .dayOfWeek(day)
                .startTime(LocalTime.parse(start))
                .endTime(LocalTime.parse(end))
                .build();
    }
}
