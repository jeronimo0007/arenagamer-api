package com.arenagamer.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Fatura gerada para compra de créditos. O saldo é creditado após o pagamento.")
public class CreditPurchaseResponse {

    @Schema(description = "ID da fatura no Perfex", example = "1234")
    private Long invoiceId;

    @Schema(description = "Quantidade de créditos a serem creditados após o pagamento", example = "50.00")
    private BigDecimal credits;

    @Schema(description = "Valor da fatura (1 crédito = R$ 1,00)", example = "50.00")
    private BigDecimal amount;

    @Schema(description = "URL de pagamento da fatura no Perfex")
    private String paymentUrl;

    @Schema(description = "Indica se uma fatura pendente existente foi reutilizada", example = "false")
    private boolean pending;
}
