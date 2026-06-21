package com.arenagamer.api.controller.admin;

import com.arenagamer.api.dto.request.WalletDepositRequest;
import com.arenagamer.api.dto.response.AdminClientWalletResponse;
import com.arenagamer.api.dto.response.AdminWalletTransactionResponse;
import com.arenagamer.api.dto.response.ApiMessages;
import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.ApiResponses;
import com.arenagamer.api.dto.response.TransactionResponse;
import com.arenagamer.api.entity.Transaction;
import com.arenagamer.api.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/wallet")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Admin / Carteira", description = "Gestão de créditos por cliente — JWT (staff)")
@SecurityRequirement(name = "Bearer")
public class AdminWalletController {

    private final WalletService walletService;

    @GetMapping("/client/{clientUserId}")
    @Operation(summary = "Carteira do cliente")
    public ResponseEntity<ApiResponse<AdminClientWalletResponse>> getClientWallet(
            @Parameter(description = "ID do cliente Perfex (userid)") @PathVariable Integer clientUserId) {
        return ApiResponses.fetched(walletService.getAdminClientWallet(clientUserId));
    }

    @GetMapping("/client/{clientUserId}/transactions")
    @Operation(summary = "Histórico de créditos do cliente")
    public ResponseEntity<ApiResponse<Page<AdminWalletTransactionResponse>>> getClientTransactions(
            @Parameter(description = "ID do cliente Perfex (userid)") @PathVariable Integer clientUserId,
            Pageable pageable) {
        return ApiResponses.listed(walletService.getClientTransactions(clientUserId, pageable));
    }

    @PostMapping("/client/{clientUserId}/deposit")
    @Operation(summary = "Adicionar créditos ao cliente")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @PathVariable Integer clientUserId,
            @Valid @RequestBody WalletDepositRequest request) {
        Transaction tx = walletService.adminDeposit(
                clientUserId, request.getAmount(), request.getDescription());
        return ApiResponses.ok(ApiMessages.DEPOSIT_SUCCESS, TransactionResponse.from(tx));
    }

    @PostMapping("/client/{clientUserId}/withdraw")
    @Operation(summary = "Remover créditos do cliente")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            @PathVariable Integer clientUserId,
            @Valid @RequestBody WalletDepositRequest request) {
        Transaction tx = walletService.adminWithdraw(
                clientUserId, request.getAmount(), request.getDescription());
        return ApiResponses.ok(ApiMessages.WITHDRAW_SUCCESS, TransactionResponse.from(tx));
    }
}
