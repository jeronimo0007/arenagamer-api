package com.arenagamer.api.controller.common;

import com.arenagamer.api.dto.response.ApiMessages;
import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.ApiResponses;
import com.arenagamer.api.security.UserPrincipal;
import com.arenagamer.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/common/auth")
@RequiredArgsConstructor
@Tag(name = "Common / Auth", description = "Autenticação — JWT (staff ou cliente)")
@SecurityRequirement(name = "Bearer")
public class CommonAuthController {

    private final AuthService authService;

    @PostMapping("/logout")
    @Operation(summary = "Encerrar sessão")
    public ResponseEntity<ApiResponse<Void>> logout() {
        authService.logout(UserPrincipal.currentType(), UserPrincipal.currentId());
        return ApiResponses.okMessage(ApiMessages.LOGOUT_SUCCESS);
    }
}
