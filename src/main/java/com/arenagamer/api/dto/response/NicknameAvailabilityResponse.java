package com.arenagamer.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Disponibilidade de nickname")
public class NicknameAvailabilityResponse {

    private String nickname;
    @Schema(description = "true = pode usar; false = já em uso")
    private Boolean available;
}
