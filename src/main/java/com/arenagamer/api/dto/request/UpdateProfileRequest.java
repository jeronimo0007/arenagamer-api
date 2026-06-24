package com.arenagamer.api.dto.request;

import com.arenagamer.api.entity.enums.Visibility;
import com.arenagamer.api.util.NicknameRules;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateProfileRequest {

    @Size(max = 50)
    private String firstName;

    @Size(max = 50)
    private String lastName;

    @Size(max = 50)
    @Pattern(regexp = NicknameRules.REGEX, message = NicknameRules.VALIDATION_MESSAGE)
    @Schema(description = "Nickname público. Letras e números apenas.")
    private String nickname;

    @Size(max = 30)
    private String phoneNumber;

    @Size(max = 500)
    private String avatarUrl;

    @Size(max = 500)
    private String instagramUrl;

    @Size(max = 500)
    private String youtubeUrl;

    @Size(max = 500)
    private String twitchUrl;

    @Schema(description = "PUBLIC, PRIVATE ou PROTECTED")
    private Visibility visibility;

    @Valid
    @Schema(description = "Substitui todos os ranks do jogador. Somente presets ativos.")
    private List<TeamRankRequest> ranks;

    @Valid
    @Schema(description = "Substitui a agenda de disponibilidade do jogador (por dia e horário).")
    private AvailabilityScheduleRequest availability;
}
