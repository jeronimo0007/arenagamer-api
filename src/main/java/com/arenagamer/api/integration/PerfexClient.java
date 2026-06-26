package com.arenagamer.api.integration;

import com.arenagamer.api.dto.response.CreditPurchaseResponse;
import com.arenagamer.api.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Cliente server-to-server para o módulo ArenaGamer no Perfex CRM.
 *
 * Usado para garantir que toda compra de créditos passe pelo Perfex: em vez de
 * creditar saldo diretamente, a API solicita ao Perfex a criação de uma fatura.
 * O saldo só é creditado depois que essa fatura é paga (o módulo Perfex chama de
 * volta {@code /admin/wallet/client/{id}/deposit} pelo hook de pagamento).
 */
@Component
public class PerfexClient {

    private static final Logger log = LoggerFactory.getLogger(PerfexClient.class);

    private final RestClient restClient;
    private final String secret;
    private final String creditInvoicePath;
    private final boolean configured;

    public PerfexClient(
            @Value("${arenagamer.perfex.base-url:}") String baseUrl,
            @Value("${arenagamer.perfex.internal-secret:}") String secret,
            @Value("${arenagamer.perfex.credit-invoice-path:/arenagamer/internal/credit_invoice}") String creditInvoicePath) {
        this.secret = secret == null ? "" : secret.trim();
        this.creditInvoicePath = creditInvoicePath;
        this.configured = StringUtils.hasText(baseUrl) && StringUtils.hasText(this.secret);
        this.restClient = StringUtils.hasText(baseUrl)
                ? RestClient.builder().baseUrl(stripTrailingSlash(baseUrl)).build()
                : RestClient.create();
    }

    /**
     * Solicita ao Perfex a criação (ou reaproveitamento) de uma fatura de compra
     * de créditos para o cliente informado.
     */
    public CreditPurchaseResponse createCreditInvoice(Integer clientUserId, Integer contactId, BigDecimal credits) {
        if (!configured) {
            throw new BusinessException(
                    "Integração com o Perfex não configurada (base-url/internal-secret).",
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
        }

        Map<String, Object> body = Map.of(
                "clientUserId", clientUserId,
                "contactId", contactId == null ? 0 : contactId,
                "credits", credits);

        PerfexCreditInvoiceResult result;
        try {
            result = restClient.post()
                    .uri(creditInvoicePath)
                    .header("X-Arena-Internal-Secret", secret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(PerfexCreditInvoiceResult.class);
        } catch (RestClientException e) {
            log.error("Falha ao criar fatura de créditos no Perfex (clientUserId={}): {}", clientUserId, e.getMessage());
            throw new BusinessException(
                    "Não foi possível gerar a fatura de créditos no Perfex.",
                    org.springframework.http.HttpStatus.BAD_GATEWAY);
        }

        if (result == null || !result.success()) {
            String message = result != null && StringUtils.hasText(result.message())
                    ? result.message()
                    : "Não foi possível gerar a fatura de créditos no Perfex.";
            throw BusinessException.badRequest(message);
        }

        return CreditPurchaseResponse.builder()
                .invoiceId(result.invoiceId())
                .credits(result.creditsAmount())
                .amount(result.amount())
                .paymentUrl(result.url())
                .pending(result.pending())
                .build();
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PerfexCreditInvoiceResult(
            boolean success,
            String message,
            @JsonProperty("invoice_id") Long invoiceId,
            @JsonProperty("credits_amount") BigDecimal creditsAmount,
            BigDecimal amount,
            String url,
            boolean pending) {
    }
}
