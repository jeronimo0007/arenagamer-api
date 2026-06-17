package com.arenagamer.api.controller;

import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.TournamentResponse;
import com.arenagamer.api.dto.response.UserResponse;
import com.arenagamer.api.entity.*;
import com.arenagamer.api.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Painel administrativo")
public class AdminController {

    private final PlanRepository planRepository;
    private final CreditTierRepository creditTierRepository;
    private final PresetRepository presetRepository;
    private final AuditLogRepository auditLogRepository;
    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;

    @GetMapping("/plans")
    @Operation(summary = "Listar planos")
    @Cacheable("plans")
    public ResponseEntity<ApiResponse<List<Plan>>> listPlans() {
        return ResponseEntity.ok(ApiResponse.ok(planRepository.findAll()));
    }

    @GetMapping("/credit-tiers")
    @Operation(summary = "Listar tiers de créditos")
    @Cacheable("creditTiers")
    public ResponseEntity<ApiResponse<List<CreditTier>>> listCreditTiers() {
        return ResponseEntity.ok(ApiResponse.ok(creditTierRepository.findAll()));
    }

    @GetMapping("/presets")
    @Operation(summary = "Listar presets de jogos")
    @Cacheable("adminPresets")
    public ResponseEntity<ApiResponse<List<Preset>>> listPresets() {
        return ResponseEntity.ok(ApiResponse.ok(presetRepository.findAll()));
    }

    @GetMapping("/audits")
    @Operation(summary = "Listar audit logs")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> listAudits(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)));
    }

    @GetMapping("/tournaments")
    @Operation(summary = "Listar todos os torneios")
    public ResponseEntity<ApiResponse<Page<TournamentResponse>>> listAllTournaments(Pageable pageable) {
        Page<TournamentResponse> page = tournamentRepository.findAll(pageable).map(TournamentResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/users")
    @Operation(summary = "Listar todos os usuários")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listAllUsers(Pageable pageable) {
        Page<UserResponse> page = userRepository.findAll(pageable).map(UserResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }
}
