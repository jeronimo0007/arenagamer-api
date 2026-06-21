package com.arenagamer.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Operação de créditos na carteira")
public class WalletDepositRequest {

    @NotNull
    @DecimalMin(value = "0.01")
    @Schema(description = "Valor em créditos", example = "50.00")
    private BigDecimal amount;

    @Schema(description = "Descrição opcional", example = "Recarga via PIX")
    private String description;
}
