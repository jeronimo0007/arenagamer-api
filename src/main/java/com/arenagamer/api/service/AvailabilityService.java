package com.arenagamer.api.service;

import com.arenagamer.api.dto.request.WeeklyAvailabilitySlotRequest;
import com.arenagamer.api.dto.response.AvailabilityScheduleResponse;
import com.arenagamer.api.entity.AvailabilityProfile;
import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.Team;
import com.arenagamer.api.entity.WeeklyAvailabilitySlot;
import com.arenagamer.api.entity.enums.TimeWindow;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.AvailabilityProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilityProfileRepository availabilityProfileRepository;

    @Transactional(readOnly = true)
    public AvailabilityScheduleResponse getClientSchedule(Integer clientUserId) {
        return availabilityProfileRepository.findByClientUserId(clientUserId)
                .map(AvailabilityScheduleResponse::from)
                .orElseGet(() -> AvailabilityScheduleResponse.builder().weeklySlots(List.of()).build());
    }

    @Transactional(readOnly = true)
    public AvailabilityScheduleResponse getTeamSchedule(Long teamId) {
        return availabilityProfileRepository.findByTeam_Id(teamId)
                .map(AvailabilityScheduleResponse::from)
                .orElseGet(() -> AvailabilityScheduleResponse.builder().weeklySlots(List.of()).build());
    }

    @Transactional
    public AvailabilityScheduleResponse syncClientSchedule(
            Integer clientUserId, Contact primaryContact, List<WeeklyAvailabilitySlotRequest> slots) {
        validateWeeklySlots(slots);
        AvailabilityProfile profile = availabilityProfileRepository.findByClientUserId(clientUserId)
                .orElseGet(() -> AvailabilityProfile.builder()
                        .clientUserId(clientUserId)
                        .contact(primaryContact)
                        .build());
        applyWeeklySlots(profile, slots);
        return AvailabilityScheduleResponse.from(availabilityProfileRepository.save(profile));
    }

    @Transactional
    public AvailabilityScheduleResponse syncTeamSchedule(Team team, List<WeeklyAvailabilitySlotRequest> slots) {
        validateWeeklySlots(slots);
        AvailabilityProfile profile = availabilityProfileRepository.findByTeam_Id(team.getId())
                .orElseGet(() -> AvailabilityProfile.builder()
                        .team(team)
                        .build());
        applyWeeklySlots(profile, slots);
        return AvailabilityScheduleResponse.from(availabilityProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public AvailabilityProfile getClientProfile(Integer clientUserId) {
        return availabilityProfileRepository.findByClientUserId(clientUserId).orElse(null);
    }

    @Transactional(readOnly = true)
    public AvailabilityProfile getTeamProfile(Long teamId) {
        return availabilityProfileRepository.findByTeam_Id(teamId).orElse(null);
    }

    /**
     * Cópia desvinculada do perfil salvo para uso na inscrição do torneio.
     */
    @Transactional
    public AvailabilityProfile snapshotForTournament(AvailabilityProfile source) {
        if (source == null || source.getWeeklySlots().isEmpty()) {
            return null;
        }
        AvailabilityProfile copy = AvailabilityProfile.builder()
                .preferWeekends(source.getPreferWeekends())
                .windows(source.getWindows() != null ? new HashSet<>(source.getWindows()) : new HashSet<>())
                .build();
        for (WeeklyAvailabilitySlot slot : source.getWeeklySlots()) {
            copy.getWeeklySlots().add(WeeklyAvailabilitySlot.builder()
                    .availabilityProfile(copy)
                    .dayOfWeek(slot.getDayOfWeek())
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .build());
        }
        return availabilityProfileRepository.save(copy);
    }

    public void validateWeeklySlots(List<WeeklyAvailabilitySlotRequest> slots) {
        if (slots == null) {
            return;
        }
        List<WeeklyAvailabilitySlotRequest> sorted = new ArrayList<>(slots);
        sorted.sort(Comparator.comparing(WeeklyAvailabilitySlotRequest::getDayOfWeek)
                .thenComparing(WeeklyAvailabilitySlotRequest::getStartTime));
        for (WeeklyAvailabilitySlotRequest slot : sorted) {
            if (slot.getDayOfWeek() == null || slot.getStartTime() == null || slot.getEndTime() == null) {
                throw BusinessException.badRequest("Cada horário deve ter dia, início e fim");
            }
            if (!slot.getStartTime().isBefore(slot.getEndTime())) {
                throw BusinessException.badRequest(
                        "Horário inválido em " + slot.getDayOfWeek() + ": início deve ser antes do fim");
            }
        }
        for (int i = 1; i < sorted.size(); i++) {
            WeeklyAvailabilitySlotRequest prev = sorted.get(i - 1);
            WeeklyAvailabilitySlotRequest curr = sorted.get(i);
            if (prev.getDayOfWeek() == curr.getDayOfWeek()
                    && prev.getEndTime().isAfter(curr.getStartTime())) {
                throw BusinessException.badRequest(
                        "Horários colidem em " + formatDay(prev.getDayOfWeek()) + ": "
                                + prev.getStartTime() + "–" + prev.getEndTime()
                                + " e " + curr.getStartTime() + "–" + curr.getEndTime());
            }
        }
    }

    public Set<TimeWindow> resolveTimeWindows(AvailabilityProfile profile) {
        if (profile == null || profile.getWeeklySlots() == null || profile.getWeeklySlots().isEmpty()) {
            if (profile != null && profile.getWindows() != null && !profile.getWindows().isEmpty()) {
                return profile.getWindows();
            }
            return EnumSet.allOf(TimeWindow.class);
        }
        Set<TimeWindow> windows = EnumSet.noneOf(TimeWindow.class);
        for (WeeklyAvailabilitySlot slot : profile.getWeeklySlots()) {
            windows.addAll(timeWindowsForRange(slot.getStartTime(), slot.getEndTime()));
        }
        return windows.isEmpty() ? EnumSet.allOf(TimeWindow.class) : windows;
    }

    public boolean isAvailableAt(AvailabilityProfile profile, DayOfWeek dayOfWeek, LocalTime time) {
        if (profile == null || profile.getWeeklySlots() == null || profile.getWeeklySlots().isEmpty()) {
            return true;
        }
        return profile.getWeeklySlots().stream()
                .filter(slot -> slot.getDayOfWeek() == dayOfWeek)
                .anyMatch(slot -> !time.isBefore(slot.getStartTime()) && time.isBefore(slot.getEndTime()));
    }

    /**
     * Faixa de horário comum (dia da semana + intervalo) entre dois perfis.
     */
    public record CommonSlot(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
    }

    /**
     * Interseção dos horários semanais de dois perfis.
     *
     * <p>Perfil {@code null} ou sem horários semanais é tratado como "disponível sempre":
     * nesse caso a interseção é igual aos horários do outro perfil. Quando ambos estão livres,
     * retorna lista vazia, sinalizando "qualquer dia/horário".</p>
     */
    public List<CommonSlot> findCommonWeeklySlots(AvailabilityProfile a, AvailabilityProfile b) {
        List<WeeklyAvailabilitySlot> slotsA = weeklySlotsOf(a);
        List<WeeklyAvailabilitySlot> slotsB = weeklySlotsOf(b);

        boolean aFree = slotsA.isEmpty();
        boolean bFree = slotsB.isEmpty();

        if (aFree && bFree) {
            return List.of();
        }
        if (aFree) {
            return toCommonSlots(slotsB);
        }
        if (bFree) {
            return toCommonSlots(slotsA);
        }

        List<CommonSlot> common = new ArrayList<>();
        for (WeeklyAvailabilitySlot slotA : slotsA) {
            for (WeeklyAvailabilitySlot slotB : slotsB) {
                if (slotA.getDayOfWeek() != slotB.getDayOfWeek()) {
                    continue;
                }
                LocalTime start = max(slotA.getStartTime(), slotB.getStartTime());
                LocalTime end = min(slotA.getEndTime(), slotB.getEndTime());
                if (start.isBefore(end)) {
                    common.add(new CommonSlot(slotA.getDayOfWeek(), start, end));
                }
            }
        }
        common.sort(Comparator.comparing(CommonSlot::dayOfWeek).thenComparing(CommonSlot::startTime));
        return common;
    }

    /**
     * Interseção das faixas do dia (TimeWindow) entre dois perfis, usada como fallback de agendamento.
     */
    public Set<TimeWindow> commonTimeWindows(AvailabilityProfile a, AvailabilityProfile b) {
        Set<TimeWindow> windows = EnumSet.copyOf(resolveTimeWindows(a));
        windows.retainAll(resolveTimeWindows(b));
        return windows;
    }

    private List<WeeklyAvailabilitySlot> weeklySlotsOf(AvailabilityProfile profile) {
        if (profile == null || profile.getWeeklySlots() == null) {
            return List.of();
        }
        return profile.getWeeklySlots();
    }

    private List<CommonSlot> toCommonSlots(List<WeeklyAvailabilitySlot> slots) {
        List<CommonSlot> result = new ArrayList<>();
        for (WeeklyAvailabilitySlot slot : slots) {
            result.add(new CommonSlot(slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime()));
        }
        result.sort(Comparator.comparing(CommonSlot::dayOfWeek).thenComparing(CommonSlot::startTime));
        return result;
    }

    private LocalTime max(LocalTime a, LocalTime b) {
        return a.isAfter(b) ? a : b;
    }

    private LocalTime min(LocalTime a, LocalTime b) {
        return a.isBefore(b) ? a : b;
    }

    private void applyWeeklySlots(AvailabilityProfile profile, List<WeeklyAvailabilitySlotRequest> slots) {
        profile.getWeeklySlots().clear();
        if (slots == null) {
            return;
        }
        for (WeeklyAvailabilitySlotRequest request : slots) {
            profile.getWeeklySlots().add(WeeklyAvailabilitySlot.builder()
                    .availabilityProfile(profile)
                    .dayOfWeek(request.getDayOfWeek())
                    .startTime(request.getStartTime())
                    .endTime(request.getEndTime())
                    .build());
        }
    }

    private String formatDay(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "segunda-feira";
            case TUESDAY -> "terça-feira";
            case WEDNESDAY -> "quarta-feira";
            case THURSDAY -> "quinta-feira";
            case FRIDAY -> "sexta-feira";
            case SATURDAY -> "sábado";
            case SUNDAY -> "domingo";
        };
    }

    private Set<TimeWindow> timeWindowsForRange(LocalTime start, LocalTime end) {
        Set<TimeWindow> windows = EnumSet.noneOf(TimeWindow.class);
        for (TimeWindow window : TimeWindow.values()) {
            LocalTime windowStart = windowStart(window);
            LocalTime windowEnd = windowEnd(window);
            if (rangesOverlap(start, end, windowStart, windowEnd)) {
                windows.add(window);
            }
        }
        return windows;
    }

    private boolean rangesOverlap(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    private LocalTime windowStart(TimeWindow window) {
        return switch (window) {
            case MORNING -> LocalTime.of(6, 0);
            case AFTERNOON -> LocalTime.of(12, 0);
            case EVENING -> LocalTime.of(18, 0);
            case NIGHT -> LocalTime.MIDNIGHT;
        };
    }

    private LocalTime windowEnd(TimeWindow window) {
        return switch (window) {
            case MORNING -> LocalTime.of(12, 0);
            case AFTERNOON -> LocalTime.of(18, 0);
            case EVENING -> LocalTime.MIDNIGHT;
            case NIGHT -> LocalTime.of(6, 0);
        };
    }
}
