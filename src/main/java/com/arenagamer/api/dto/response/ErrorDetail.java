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
@Schema(description = "Detalhe de erro de um campo específico")
public class ErrorDetail {

    @Schema(example = "email")
    private String field;

    @Schema(example = "must be a well-formed email address")
    private String message;

    public static ErrorDetail of(String field, String message) {
        return ErrorDetail.builder().field(field).message(message).build();
    }
}
