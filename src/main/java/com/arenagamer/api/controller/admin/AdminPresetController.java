package com.arenagamer.api.controller.admin;

import com.arenagamer.api.dto.request.PresetRequest;
import com.arenagamer.api.dto.response.ApiMessages;
import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.ApiResponses;
import com.arenagamer.api.dto.response.PresetResponse;
import com.arenagamer.api.entity.Preset;
import com.arenagamer.api.service.PresetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/presets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Admin / Presets", description = "Gerenciamento de presets de jogos — JWT (staff)")
@SecurityRequirement(name = "Bearer")
public class AdminPresetController {

    private final PresetService presetService;

    @GetMapping
    @Operation(summary = "Listar ou pesquisar presets",
            description = """
                    Sem q: todos os presets (ativos e inativos).
                    Com q: filtra por nome do jogo ou plataforma.
                    activeOnly=true limita aos ativos.""")
    public ResponseEntity<ApiResponse<List<PresetResponse>>> listPresets(
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<PresetResponse> presets = presetService.search(q, activeOnly).stream()
                .map(PresetResponse::from)
                .toList();
        return ApiResponses.listed(presets);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhes do preset")
    public ResponseEntity<ApiResponse<PresetResponse>> getPreset(@PathVariable Long id) {
        return ApiResponses.fetched(PresetResponse.from(presetService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Criar preset")
    public ResponseEntity<ApiResponse<PresetResponse>> createPreset(@Valid @RequestBody PresetRequest request) {
        Preset preset = presetService.create(request);
        return ApiResponses.created(ApiMessages.PRESET_CREATED, PresetResponse.from(preset));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar preset")
    public ResponseEntity<ApiResponse<PresetResponse>> updatePreset(
            @PathVariable Long id, @Valid @RequestBody PresetRequest request) {
        Preset preset = presetService.update(id, request);
        return ApiResponses.updated(ApiMessages.PRESET_UPDATED, PresetResponse.from(preset));
    }
}
