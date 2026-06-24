package com.arenagamer.api.controller.common;

import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.ApiResponses;
import com.arenagamer.api.dto.response.MyTeamRankResponse;
import com.arenagamer.api.security.UserPrincipal;
import com.arenagamer.api.service.TeamRankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/common/teams/ranks")
@RequiredArgsConstructor
@Tag(name = "Common / Times / Rank", description = "Rank do meu cliente por jogo (preset)")
@SecurityRequirement(name = "Bearer")
public class CommonTeamRankController {

    private final TeamRankService teamRankService;

    @GetMapping("/me")
    @Operation(
            summary = "Meu rank",
            description = "Posição global e regional do(s) time(s) do meu cliente. Filtro opcional por preset (jogo).")
    public ResponseEntity<ApiResponse<MyTeamRankResponse>> myRanks(
            @Parameter(description = "ID do preset (jogo). Omitir = todos os jogos do meu time.")
            @RequestParam(required = false) Long presetId) {
        return ApiResponses.fetched(teamRankService.getMyRanks(UserPrincipal.current(), presetId));
    }
}
