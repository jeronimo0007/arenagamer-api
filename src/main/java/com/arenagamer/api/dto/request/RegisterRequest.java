package com.arenagamer.api.dto.request;

import com.arenagamer.api.util.NicknameRules;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dados de cadastro de novo cliente")
public class RegisterRequest {

    @NotBlank @Email
    @Schema(description = "E-mail", example = "cliente@exemplo.com")
    private String email;

    @NotBlank @Size(min = 6, max = 100)
    @Schema(description = "Senha (mínimo 6 caracteres)", example = "senha123")
    private String password;

    @NotBlank @Size(max = 50)
    @Schema(description = "Nome", example = "João")
    private String firstName;

    @NotBlank @Size(max = 50)
    @Schema(description = "Sobrenome", example = "Silva")
    private String lastName;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = NicknameRules.REGEX, message = NicknameRules.VALIDATION_MESSAGE)
    @Schema(description = "Nickname público (único). Letras e números apenas.", example = "joaosilva")
    private String nickname;

    @Size(max = 30)
    @Schema(description = "Telefone (opcional)", example = "11999999999")
    private String phoneNumber;

    @Size(max = 500)
    @Schema(description = "URL da foto de perfil (opcional)")
    private String avatarUrl;

    @Size(max = 500)
    private String instagramUrl;

    @Size(max = 500)
    private String youtubeUrl;

    @Size(max = 500)
    private String twitchUrl;

    @Schema(description = "Fuso horário (opcional)", example = "America/Sao_Paulo")
    private String timezone;
}
