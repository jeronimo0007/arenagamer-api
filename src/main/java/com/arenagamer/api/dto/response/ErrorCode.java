package com.arenagamer.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "Código padronizado de erro da API")
public enum ErrorCode {

    VALIDATION_ERROR("VALIDATION_ERROR", "Dados inválidos"),
    BAD_REQUEST("BAD_REQUEST", "Requisição inválida"),
    UNAUTHORIZED("UNAUTHORIZED", "Não autenticado"),
    FORBIDDEN("FORBIDDEN", "Acesso negado"),
    NOT_FOUND("NOT_FOUND", "Recurso não encontrado"),
    ENDPOINT_NOT_FOUND("ENDPOINT_NOT_FOUND", "Endpoint não encontrado"),
    CONFLICT("CONFLICT", "Conflito de dados"),
    INTERNAL_ERROR("INTERNAL_ERROR", "Erro interno do servidor");

    private final String code;
    private final String defaultMessage;
}
