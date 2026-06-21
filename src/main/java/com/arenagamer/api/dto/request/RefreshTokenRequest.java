package com.arenagamer.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Token de renovação de sessão")
public class RefreshTokenRequest {

    @NotBlank
    @Schema(description = "Refresh token JWT", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;
}
