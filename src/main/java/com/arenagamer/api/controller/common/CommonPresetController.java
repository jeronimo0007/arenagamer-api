package com.arenagamer.api.controller.common;

import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.ApiResponses;
import com.arenagamer.api.dto.response.PresetResponse;
import com.arenagamer.api.service.PresetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/common/presets")
@RequiredArgsConstructor
@Validated
@Tag(name = "Common / Presets", description = "Jogos (presets) — JWT")
@SecurityRequirement(name = "Bearer")
public class CommonPresetController {

    private final PresetService presetService;

    @GetMapping
    @Operation(summary = "Listar ou pesquisar presets (jogos)",
            description = """
                    Sem q: todos os presets ativos, ordenados por nome do jogo.
                    Com q: filtra por nome do jogo ou plataforma (contém, case insensitive).
                    Use no autocomplete ao criar torneio ou escolher rank.""")
    public ResponseEntity<ApiResponse<List<PresetResponse>>> searchPresets(
            @Parameter(description = "Texto digitado — busca no nome do jogo ou plataforma")
            @RequestParam(required = false)
            @Size(max = 100)
            String q) {
        List<PresetResponse> presets = presetService.search(q, true).stream()
                .map(PresetResponse::from)
                .toList();
        return ApiResponses.listed(presets);
    }
}
