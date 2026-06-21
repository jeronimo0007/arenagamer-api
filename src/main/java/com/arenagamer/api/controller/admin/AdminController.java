package com.arenagamer.api.controller.admin;

import com.arenagamer.api.dto.request.AdminAuditLogRequest;
import com.arenagamer.api.dto.response.*;
import com.arenagamer.api.repository.*;
import com.arenagamer.api.security.AuthenticatedUser;
import com.arenagamer.api.service.AuditService;
import com.arenagamer.api.service.TournamentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Admin / Painel", description = "Painel administrativo — JWT (staff)")
@SecurityRequirement(name = "Bearer")
public class AdminController {

    private final AuditService auditService;
    private final TournamentService tournamentService;
    private final AuditLogRepository auditLogRepository;
    private final TournamentRepository tournamentRepository;
    private final StaffRepository staffRepository;
    private final ContactRepository contactRepository;

    @GetMapping("/audits")
    @Operation(summary = "Listar audit logs")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> listAudits(Pageable pageable) {
        Page<AuditLogResponse> page = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(AuditLogResponse::from);
        return ApiResponses.listed(page);
    }

    @PostMapping("/audits")
    @Operation(summary = "Registrar auditoria manual (staff)")
    public ResponseEntity<ApiResponse<AuditLogResponse>> createAudit(@Valid @RequestBody AdminAuditLogRequest request) {
        auditService.recordStaffAction(
                request.getAction(),
                request.getEntityType(),
                request.getEntityId(),
                request.getOldValue(),
                request.getNewValue());
        AuditLogResponse latest = auditLogRepository.findAllByOrderByCreatedAtDesc(Pageable.ofSize(1))
                .map(AuditLogResponse::from)
                .getContent()
                .stream()
                .findFirst()
                .orElse(null);
        return ApiResponses.created(ApiMessages.CREATE_SUCCESS, latest);
    }

    @GetMapping("/tournaments")
    @Operation(summary = "Listar todos os torneios")
    public ResponseEntity<ApiResponse<Page<TournamentResponse>>> listAllTournaments(Pageable pageable) {
        Page<TournamentResponse> page = tournamentService.toResponsePage(tournamentRepository.findAll(pageable));
        return ApiResponses.listed(page);
    }

    @GetMapping("/users")
    @Operation(summary = "Listar staff")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listAllUsers(Pageable pageable) {
        Page<UserResponse> staff = staffRepository.findAll(pageable)
                .map(s -> UserResponse.from(AuthenticatedUser.fromStaff(s)));
        return ApiResponses.listed(staff);
    }

    @GetMapping("/contacts")
    @Operation(summary = "Listar contatos de clientes")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listContacts(Pageable pageable) {
        Page<UserResponse> contacts = contactRepository.findAll(pageable)
                .map(c -> UserResponse.from(AuthenticatedUser.fromContact(c)));
        return ApiResponses.listed(contacts);
    }
}
