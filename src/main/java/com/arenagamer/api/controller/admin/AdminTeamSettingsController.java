package com.arenagamer.api.controller.admin;

import com.arenagamer.api.dto.request.TeamSettingsRequest;
import com.arenagamer.api.dto.response.ApiMessages;
import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.ApiResponses;
import com.arenagamer.api.dto.response.TeamSettingsResponse;
import com.arenagamer.api.entity.TeamSettings;
import com.arenagamer.api.service.TeamSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/team-settings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Admin / Configurações de times", description = "Limites de propriedade, participação e torneios por time")
@SecurityRequirement(name = "Bearer")
public class AdminTeamSettingsController {

    private final TeamSettingsService teamSettingsService;

    @GetMapping
    @Operation(summary = "Obter configurações de times")
    public ResponseEntity<ApiResponse<TeamSettingsResponse>> get() {
        TeamSettings settings = teamSettingsService.getSettings();
        return ApiResponses.fetched(TeamSettingsResponse.from(settings));
    }

    @PutMapping
    @Operation(summary = "Atualizar configurações de times")
    public ResponseEntity<ApiResponse<TeamSettingsResponse>> update(
            @Valid @RequestBody TeamSettingsRequest request) {
        TeamSettings settings = teamSettingsService.update(request);
        return ApiResponses.updated(ApiMessages.TEAM_SETTINGS_UPDATED, TeamSettingsResponse.from(settings));
    }
}
