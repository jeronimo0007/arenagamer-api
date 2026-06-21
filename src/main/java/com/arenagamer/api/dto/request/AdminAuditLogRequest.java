package com.arenagamer.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Registro manual de auditoria (ex.: ações no Perfex)")
public class AdminAuditLogRequest {

    @NotBlank
    @Schema(description = "Ação executada", example = "UPDATE")
    private String action;

    @NotBlank
    @Schema(description = "Tipo da entidade", example = "settings")
    private String entityType;

    @Schema(description = "ID da entidade", example = "0")
    private Long entityId;

    @Schema(description = "Valor anterior (JSON ou texto)")
    private String oldValue;

    @Schema(description = "Valor novo (JSON ou texto)")
    private String newValue;
}
