package com.arenagamer.api.controller.publicapi;

import com.arenagamer.api.dto.request.LoginRequest;
import com.arenagamer.api.dto.request.RefreshTokenRequest;
import com.arenagamer.api.dto.request.RegisterRequest;
import com.arenagamer.api.dto.response.ApiMessages;
import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.ApiResponses;
import com.arenagamer.api.dto.response.AuthResponse;
import com.arenagamer.api.dto.response.NicknameAvailabilityResponse;
import com.arenagamer.api.service.AuthService;
import com.arenagamer.api.service.ClientNicknameService;
import com.arenagamer.api.util.NicknameRules;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Public / Auth", description = "Cadastro e login — sem autenticação")
public class PublicAuthController {

    private final AuthService authService;
    private final ClientNicknameService clientNicknameService;

    @GetMapping("/nickname-available")
    @Operation(summary = "Verificar se nickname está disponível", description = "Letras e números apenas. Comparação case-insensitive.")
    public ResponseEntity<ApiResponse<NicknameAvailabilityResponse>> checkNickname(
            @Parameter(description = "Nickname desejado", example = "joaosilva")
            @RequestParam
            @NotBlank
            @Size(max = 50)
            @Pattern(regexp = NicknameRules.REGEX, message = NicknameRules.VALIDATION_MESSAGE)
            String nickname) {
        return ApiResponses.fetched(clientNicknameService.checkAvailability(nickname, null));
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar novo cliente")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponses.created(ApiMessages.REGISTER_SUCCESS, authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login (staff ou cliente)")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponses.ok(ApiMessages.LOGIN_SUCCESS, authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar token de acesso")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponses.ok(ApiMessages.REFRESH_SUCCESS, authService.refresh(request.getRefreshToken()));
    }
}
