package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.Team;
import com.arenagamer.api.entity.Client;
import com.arenagamer.api.entity.TeamRank;
import com.arenagamer.api.entity.enums.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeamResponse {

    private Long id;
    private String name;
    private String tag;
    private String logoUrl;
    private String bannerUrl;
    private String youtubeUrl;
    private String instagramUrl;
    private String twitchUrl;
    private String otherSocialUrl;
    private String description;
    @Schema(description = "PUBLIC, PRIVATE ou PROTECTED")
    private Visibility visibility;
    @Schema(description = "ID do cliente (empresa) dono do time")
    private Integer clientUserId;
    @Schema(description = "Nome da empresa dona do time")
    private String clientName;
    private Integer memberCount;
    @Schema(description = "Contato logado pode inscrever este time em torneios (dono/capitão)")
    private Boolean canRegisterInTournament;
    @Schema(description = "Ranks por jogo. Omitido em times privados para não-membros.")
    private List<TeamRankResponse> ranks;
    private LocalDateTime createdAt;

    public static TeamResponse from(Team team) {
        return from(team, null, false);
    }

    public static TeamResponse from(Team team, List<TeamRank> ranks, boolean includeRanks) {
        Client client = team.getClient();
        Integer clientUserId = null;
        String clientName = null;
        if (client != null) {
            clientUserId = client.getUserId();
            clientName = client.getCompany();
        }

        List<TeamRankResponse> rankResponses = null;
        if (includeRanks && ranks != null) {
            rankResponses = ranks.stream().map(TeamRankResponse::from).toList();
        }

        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .tag(team.getTag())
                .logoUrl(team.getLogoUrl())
                .bannerUrl(team.getBannerUrl())
                .youtubeUrl(team.getYoutubeUrl())
                .instagramUrl(team.getInstagramUrl())
                .twitchUrl(team.getTwitchUrl())
                .otherSocialUrl(team.getOtherSocialUrl())
                .description(team.getDescription())
                .visibility(team.getVisibility())
                .clientUserId(clientUserId)
                .clientName(clientName)
                .memberCount(team.getMembers() != null ? team.getMembers().size() : 0)
                .ranks(rankResponses)
                .createdAt(team.getCreatedAt())
                .build();
    }
}
