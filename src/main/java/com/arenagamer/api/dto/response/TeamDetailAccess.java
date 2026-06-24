package com.arenagamer.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Nível de detalhe retornado conforme visibilidade e vínculo do viewer")
public enum TeamDetailAccess {
    @Schema(description = "Detalhes completos")
    FULL,
    @Schema(description = "Resumo para times PROTECTED (não vinculados)")
    PROTECTED_SUMMARY,
    @Schema(description = "Time PRIVATE — viewer não vinculado")
    PRIVATE_RESTRICTED
}
