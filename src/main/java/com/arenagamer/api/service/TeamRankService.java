package com.arenagamer.api.service;

import com.arenagamer.api.dto.response.MyTeamRankResponse;
import com.arenagamer.api.dto.response.TeamLeaderboardEntryResponse;
import com.arenagamer.api.dto.response.TeamPerformanceResponse;
import com.arenagamer.api.dto.response.TeamRankPositionResponse;
import com.arenagamer.api.entity.Client;
import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.Team;
import com.arenagamer.api.entity.TeamRank;
import com.arenagamer.api.entity.enums.Visibility;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.ClientRepository;
import com.arenagamer.api.repository.PresetRepository;
import com.arenagamer.api.repository.TeamRankRepository;
import com.arenagamer.api.security.AuthenticatedUser;
import com.arenagamer.api.util.TeamVisibilityRules;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamRankService {

    private final TeamRankRepository teamRankRepository;
    private final PresetRepository presetRepository;
    private final ClientRepository clientRepository;
    private final IdentityService identityService;

    @Transactional(readOnly = true)
    public MyTeamRankResponse getMyRanks(AuthenticatedUser auth, Long presetId) {
        Contact contact = identityService.requireContact(auth);
        if (presetId != null) {
            presetRepository.findByIdAndActiveTrue(presetId)
                    .orElseThrow(() -> BusinessException.notFound("Preset não encontrado ou inativo"));
        }

        Client client = clientRepository.findById(contact.getUserid())
                .orElseThrow(() -> BusinessException.notFound("Cliente não encontrado"));

        List<TeamRank> teamRanks = teamRankRepository.findByClientUserIdAndOptionalPreset(
                contact.getUserid(), presetId);

        List<TeamRankPositionResponse> entries = new ArrayList<>();
        for (TeamRank teamRank : teamRanks) {
            entries.add(toPositionResponse(teamRank));
        }

        return MyTeamRankResponse.builder()
                .clientUserId(client.getUserId())
                .clientName(client.getCompany())
                .region(client.getState())
                .ranks(entries)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<TeamLeaderboardEntryResponse> getGlobalLeaderboard(Long presetId, Pageable pageable) {
        validatePreset(presetId);
        Page<TeamRank> page = teamRankRepository.findPublicLeaderboard(presetId, TeamVisibilityRules.RANKED, pageable);
        return mapLeaderboardPage(page, pageable);
    }

    @Transactional(readOnly = true)
    public Page<TeamLeaderboardEntryResponse> getRegionalLeaderboard(Long presetId, String state, Pageable pageable) {
        validatePreset(presetId);
        String normalizedState = normalizeRegion(state);
        Page<TeamRank> page = teamRankRepository.findPublicRegionalLeaderboard(
                presetId, TeamVisibilityRules.RANKED, normalizedState, pageable);
        return mapLeaderboardPage(page, pageable);
    }

    private Page<TeamLeaderboardEntryResponse> mapLeaderboardPage(Page<TeamRank> page, Pageable pageable) {
        List<TeamLeaderboardEntryResponse> entries = new ArrayList<>();
        for (int i = 0; i < page.getContent().size(); i++) {
            entries.add(toLeaderboardEntry(page.getContent().get(i), pageable.getOffset() + i + 1));
        }
        return new PageImpl<>(entries, pageable, page.getTotalElements());
    }

    private TeamRankPositionResponse toPositionResponse(TeamRank teamRank) {
        var team = teamRank.getTeam();
        var client = team.getClient();
        var preset = teamRank.getPreset();
        String region = client != null ? client.getState() : null;

        Long globalPosition = null;
        Long regionalPosition = null;
        if (TeamVisibilityRules.RANKED.contains(team.getVisibility())) {
            globalPosition = teamRankRepository.countGlobalPosition(
                    preset.getId(), TeamVisibilityRules.RANKED, teamRank.getRankPoints(), team.getId());
            if (region != null && !region.isBlank()) {
                regionalPosition = teamRankRepository.countRegionalPosition(
                        preset.getId(), TeamVisibilityRules.RANKED, region, teamRank.getRankPoints(), team.getId());
            }
        }

        return TeamRankPositionResponse.builder()
                .teamId(team.getId())
                .teamName(team.getName())
                .teamTag(team.getTag())
                .clientUserId(client != null ? client.getUserId() : null)
                .clientName(client != null ? client.getCompany() : null)
                .region(region)
                .presetId(preset.getId())
                .gameName(preset.getGameName())
                .rankPoints(teamRank.getRankPoints())
                .globalPosition(globalPosition)
                .regionalPosition(regionalPosition)
                .build();
    }

    public TeamPerformanceResponse toPerformance(TeamRank teamRank) {
        var team = teamRank.getTeam();
        var preset = teamRank.getPreset();
        var client = team.getClient();
        String region = client != null ? client.getState() : null;

        Long globalPosition = null;
        Long regionalPosition = null;
        if (TeamVisibilityRules.RANKED.contains(team.getVisibility())) {
            globalPosition = teamRankRepository.countGlobalPosition(
                    preset.getId(), TeamVisibilityRules.RANKED, teamRank.getRankPoints(), team.getId());
            if (region != null && !region.isBlank()) {
                regionalPosition = teamRankRepository.countRegionalPosition(
                        preset.getId(), TeamVisibilityRules.RANKED, region, teamRank.getRankPoints(), team.getId());
            }
        }

        return TeamPerformanceResponse.builder()
                .presetId(preset.getId())
                .gameName(preset.getGameName())
                .platform(preset.getPlatform())
                .rankPoints(teamRank.getRankPoints())
                .globalPosition(globalPosition)
                .regionalPosition(regionalPosition)
                .build();
    }

    private TeamLeaderboardEntryResponse toLeaderboardEntry(TeamRank teamRank, long position) {
        var team = teamRank.getTeam();
        var client = team.getClient();
        return TeamLeaderboardEntryResponse.builder()
                .position(position)
                .teamId(team.getId())
                .teamName(team.getName())
                .teamTag(team.getTag())
                .clientUserId(client.getUserId())
                .clientName(client.getCompany())
                .region(client.getState())
                .rankPoints(teamRank.getRankPoints())
                .build();
    }

    private void validatePreset(Long presetId) {
        presetRepository.findByIdAndActiveTrue(presetId)
                .orElseThrow(() -> BusinessException.notFound("Preset não encontrado ou inativo"));
    }

    private String normalizeRegion(String state) {
        if (state == null || state.isBlank()) {
            throw BusinessException.badRequest("Estado (região) é obrigatório para ranking regional");
        }
        return state.trim();
    }
}
