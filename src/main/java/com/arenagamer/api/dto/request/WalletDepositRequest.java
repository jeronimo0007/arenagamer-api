package com.arenagamer.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletDepositRequest {

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    private String description;
}
