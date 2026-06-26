package com.arenagamer.api.controller.common;

import com.arenagamer.api.dto.request.ContactWalletPermissionRequest;
import com.arenagamer.api.dto.request.WalletDepositRequest;
import com.arenagamer.api.dto.response.ApiMessages;
import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.ApiResponses;
import com.arenagamer.api.dto.response.ClientWalletResponse;
import com.arenagamer.api.dto.response.ContactWalletPermissionResponse;
import com.arenagamer.api.dto.response.CreditPurchaseResponse;
import com.arenagamer.api.dto.response.TransactionResponse;
import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.Transaction;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.ContactRepository;
import com.arenagamer.api.security.UserPrincipal;
import com.arenagamer.api.service.WalletAccessService;
import com.arenagamer.api.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/common/wallet")
@RequiredArgsConstructor
@Tag(name = "Common / Carteira", description = "Carteira de créditos da empresa — JWT (cliente)")
@SecurityRequirement(name = "Bearer")
public class CommonWalletController {

    private final WalletService walletService;
    private final WalletAccessService walletAccessService;
    private final ContactRepository contactRepository;

    @GetMapping("/balance")
    @Operation(summary = "Consultar saldo da empresa")
    public ResponseEntity<ApiResponse<ClientWalletResponse>> balance() {
        Contact contact = requireContact();
        return ApiResponses.fetched(walletService.getClientWalletForContact(contact));
    }

    @PostMapping("/credits/purchase")
    @Operation(summary = "Comprar créditos (gera fatura no Perfex)",
            description = "Toda compra de créditos passa pelo Perfex: gera uma fatura e retorna a URL de "
                    + "pagamento. O saldo só é creditado automaticamente após o pagamento da fatura.")
    public ResponseEntity<ApiResponse<CreditPurchaseResponse>> purchaseCredits(
            @Valid @RequestBody WalletDepositRequest request) {
        Contact contact = requireContact();
        CreditPurchaseResponse purchase = walletService.purchaseCreditsForContact(contact, request.getAmount());
        return ApiResponses.ok(ApiMessages.CREDIT_INVOICE_CREATED, purchase);
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Sacar créditos da carteira da empresa")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(@Valid @RequestBody WalletDepositRequest request) {
        Contact contact = requireContact();
        Transaction tx = walletService.withdrawForContact(
                contact, request.getAmount(), request.getDescription());
        return ApiResponses.ok(ApiMessages.WITHDRAW_SUCCESS, TransactionResponse.from(tx));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Histórico de transações da empresa")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> transactions(Pageable pageable) {
        Contact contact = requireContact();
        Page<TransactionResponse> page = walletService.getTransactionsForContact(contact, pageable);
        return ApiResponses.listed(page);
    }

    @GetMapping("/permissions")
    @Operation(summary = "Listar permissões de créditos dos contatos secundários")
    public ResponseEntity<ApiResponse<List<ContactWalletPermissionResponse>>> listPermissions() {
        Contact contact = requireContact();
        return ApiResponses.listed(walletAccessService.listSecondaryPermissions(contact));
    }

    @PutMapping("/permissions/{contactId}")
    @Operation(summary = "Atualizar permissões de créditos de um contato secundário")
    public ResponseEntity<ApiResponse<ContactWalletPermissionResponse>> updatePermissions(
            @PathVariable Integer contactId,
            @Valid @RequestBody ContactWalletPermissionRequest request) {
        Contact contact = requireContact();
        return ApiResponses.updated(
                "Permissões de créditos atualizadas",
                walletAccessService.updateSecondaryPermissions(contact, contactId, request));
    }

    private Contact requireContact() {
        if (!UserPrincipal.current().isContact()) {
            throw BusinessException.forbidden("Carteira disponível apenas para clientes");
        }
        return contactRepository.findById(UserPrincipal.currentContactId())
                .orElseThrow(() -> BusinessException.notFound("Contato não encontrado"));
    }
}
