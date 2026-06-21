package com.arenagamer.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resposta padronizada de sucesso da API")
public class ApiResponse<T> {

    @Builder.Default
    @Schema(example = "true")
    private boolean success = true;

    @Builder.Default
    @Schema(example = "Operação realizada com sucesso")
    private String message = ApiMessages.SUCCESS;

    @Builder.Default
    private Instant timestamp = Instant.now();

    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder().data(data).build();
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder().message(message).data(data).build();
    }

    public static <T> ApiResponse<T> ok(String message) {
        return ApiResponse.<T>builder().message(message).build();
    }
}
