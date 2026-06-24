package com.arenagamer.api.controller.common;

import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.ApiResponses;
import com.arenagamer.api.dto.response.PlayerDetailResponse;
import com.arenagamer.api.security.UserPrincipal;
import com.arenagamer.api.service.PlayerProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/common/players")
@RequiredArgsConstructor
@Tag(name = "Common / Jogadores", description = "Perfil do jogador (cliente) — JWT")
@SecurityRequirement(name = "Bearer")
public class CommonPlayerController {

    private final PlayerProfileService playerProfileService;

    @GetMapping("/{clientUserId}")
    @Operation(
            summary = "Detalhes do jogador",
            description = """
                    Mesmas regras de visibilidade dos times.
                    Rank principal = jogo mais jogado; use presetId para filtrar.""")
    public ResponseEntity<ApiResponse<PlayerDetailResponse>> getPlayer(
            @Parameter(description = "ID do jogador (clientUserId)") @PathVariable Integer clientUserId,
            @Parameter(description = "Preset (jogo) para o rank exibido") @RequestParam(required = false) Long presetId) {
        return ApiResponses.fetched(
                playerProfileService.getPlayerDetails(clientUserId, UserPrincipal.current(), presetId));
    }
}
