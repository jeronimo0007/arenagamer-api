package com.arenagamer.api.controller;

import com.arenagamer.api.dto.request.WalletDepositRequest;
import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.TransactionResponse;
import com.arenagamer.api.dto.response.WalletResponse;
import com.arenagamer.api.entity.Transaction;
import com.arenagamer.api.entity.Wallet;
import com.arenagamer.api.security.UserPrincipal;
import com.arenagamer.api.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Carteira de créditos")
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/balance")
    @Operation(summary = "Consultar saldo")
    public ResponseEntity<ApiResponse<WalletResponse>> balance() {
        Wallet wallet = walletService.getWallet(UserPrincipal.currentId());
        return ResponseEntity.ok(ApiResponse.ok(WalletResponse.from(wallet)));
    }

    @PostMapping("/deposit")
    @Operation(summary = "Depositar créditos")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(@Valid @RequestBody WalletDepositRequest request) {
        Transaction tx = walletService.deposit(UserPrincipal.currentId(), request.getAmount(), request.getDescription());
        return ResponseEntity.ok(ApiResponse.ok("Depósito realizado", TransactionResponse.from(tx)));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Sacar créditos")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(@Valid @RequestBody WalletDepositRequest request) {
        Transaction tx = walletService.withdraw(UserPrincipal.currentId(), request.getAmount(), request.getDescription());
        return ResponseEntity.ok(ApiResponse.ok("Saque realizado", TransactionResponse.from(tx)));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Histórico de transações")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> transactions(Pageable pageable) {
        Page<TransactionResponse> page = walletService.getTransactions(UserPrincipal.currentId(), pageable)
                .map(TransactionResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }
}
