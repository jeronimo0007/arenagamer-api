package com.arenagamer.api.service;

import com.arenagamer.api.dto.request.TeamRankRequest;
import com.arenagamer.api.dto.response.TeamPerformanceResponse;
import com.arenagamer.api.entity.Client;
import com.arenagamer.api.entity.ClientRank;
import com.arenagamer.api.entity.Preset;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.ClientRankRepository;
import com.arenagamer.api.repository.PresetRepository;
import com.arenagamer.api.util.TeamVisibilityRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClientRankService {

    private final ClientRankRepository clientRankRepository;
    private final PresetRepository presetRepository;

    public TeamPerformanceResponse toPerformance(ClientRank clientRank) {
        Client client = clientRank.getClient();
        Preset preset = clientRank.getPreset();
        String region = client.getState();

        Long globalPosition = null;
        Long regionalPosition = null;
        if (TeamVisibilityRules.RANKED.contains(client.getVisibility())) {
            globalPosition = clientRankRepository.countGlobalPosition(
                    preset.getId(),
                    TeamVisibilityRules.RANKED,
                    clientRank.getRankPoints(),
                    client.getUserId());
            if (region != null && !region.isBlank()) {
                regionalPosition = clientRankRepository.countRegionalPosition(
                        preset.getId(),
                        TeamVisibilityRules.RANKED,
                        region,
                        clientRank.getRankPoints(),
                        client.getUserId());
            }
        }

        return TeamPerformanceResponse.builder()
                .presetId(preset.getId())
                .gameName(preset.getGameName())
                .platform(preset.getPlatform())
                .rankPoints(clientRank.getRankPoints())
                .globalPosition(globalPosition)
                .regionalPosition(regionalPosition)
                .build();
    }

    @Transactional
    public void syncRanks(Client client, List<TeamRankRequest> requests) {
        if (requests == null) {
            return;
        }

        if (requests.isEmpty()) {
            clientRankRepository.deleteByClient_UserId(client.getUserId());
            return;
        }

        Set<Long> presetIds = new HashSet<>();
        for (TeamRankRequest request : requests) {
            if (!presetIds.add(request.getPresetId())) {
                throw BusinessException.badRequest("Cada jogo (preset) pode aparecer apenas uma vez nos ranks");
            }

            Preset preset = presetRepository.findByIdAndActiveTrue(request.getPresetId())
                    .orElseThrow(() -> BusinessException.badRequest(
                            "Preset inválido ou inativo: " + request.getPresetId()));

            ClientRank rank = clientRankRepository
                    .findByClient_UserIdAndPreset_Id(client.getUserId(), preset.getId())
                    .orElseGet(() -> ClientRank.builder().client(client).preset(preset).build());
            rank.setRankPoints(request.getRankPoints());
            clientRankRepository.save(rank);
        }

        clientRankRepository.deleteByClient_UserIdAndPreset_IdNotIn(client.getUserId(), presetIds);
    }
}
