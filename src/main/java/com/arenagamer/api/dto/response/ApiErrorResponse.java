package com.arenagamer.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resposta padronizada de erro da API")
public class ApiErrorResponse {

    @Builder.Default
    @Schema(example = "false")
    private boolean success = false;

    @Schema(example = "VALIDATION_ERROR")
    private String code;

    @Schema(example = "Dados inválidos")
    private String message;

    @Schema(example = "400")
    private int status;

    @Builder.Default
    private Instant timestamp = Instant.now();

    private List<ErrorDetail> errors;

    public static ApiErrorResponse of(ErrorCode errorCode, HttpStatus status) {
        return of(errorCode, errorCode.getDefaultMessage(), status, null);
    }

    public static ApiErrorResponse of(ErrorCode errorCode, String message, HttpStatus status) {
        return of(errorCode, message, status, null);
    }

    public static ApiErrorResponse of(ErrorCode errorCode, String message, HttpStatus status, List<ErrorDetail> errors) {
        return ApiErrorResponse.builder()
                .code(errorCode.getCode())
                .message(message)
                .status(status.value())
                .errors(errors)
                .build();
    }
}
