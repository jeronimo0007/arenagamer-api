package com.arenagamer.api.controller;

import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.entity.Preset;
import com.arenagamer.api.repository.PresetRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/presets")
@RequiredArgsConstructor
@Tag(name = "Presets", description = "Configurações de jogos")
public class PresetController {

    private final PresetRepository presetRepository;

    @GetMapping
    @Operation(summary = "Listar presets disponíveis")
    @Cacheable("presets")
    public ResponseEntity<ApiResponse<List<Preset>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(presetRepository.findByActiveTrue()));
    }
}
