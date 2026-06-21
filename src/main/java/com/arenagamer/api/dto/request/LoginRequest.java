package com.arenagamer.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Credenciais de login")
public class LoginRequest {

    @NotBlank @Email
    @Schema(description = "E-mail do usuário", example = "usuario@exemplo.com")
    private String email;

    @NotBlank
    @Schema(description = "Senha", example = "senha123")
    private String password;

    @Schema(description = "true = staff (tblstaff), false ou omitido = cliente (tblcontacts)", example = "false")
    private Boolean staff;
}
