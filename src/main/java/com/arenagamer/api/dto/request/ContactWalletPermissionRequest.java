package com.arenagamer.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Permissões de créditos para contato secundário")
public class ContactWalletPermissionRequest {

    @NotNull
    @Schema(description = "Permite visualizar saldo e histórico")
    private Boolean walletViewAllowed;

    @NotNull
    @Schema(description = "Permite usar créditos (torneios, taxas etc.)")
    private Boolean walletUseAllowed;
}
