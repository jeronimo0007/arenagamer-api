package com.arenagamer.api.service;

import com.arenagamer.api.dto.response.AdminClientWalletResponse;
import com.arenagamer.api.dto.response.AdminWalletTransactionResponse;
import com.arenagamer.api.dto.response.ClientWalletResponse;
import com.arenagamer.api.dto.response.CreditPurchaseResponse;
import com.arenagamer.api.dto.response.TransactionResponse;
import com.arenagamer.api.integration.PerfexClient;
import com.arenagamer.api.entity.Client;
import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.Transaction;
import com.arenagamer.api.entity.Wallet;
import com.arenagamer.api.entity.enums.TransactionStatus;
import com.arenagamer.api.entity.enums.TransactionType;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.ClientRepository;
import com.arenagamer.api.repository.ContactRepository;
import com.arenagamer.api.repository.TransactionRepository;
import com.arenagamer.api.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final ContactRepository contactRepository;
    private final ClientRepository clientRepository;
    private final WalletAccessService walletAccessService;
    private final AuditService auditService;
    private final PerfexClient perfexClient;

    public ClientWalletResponse getClientWalletForContact(Contact contact) {
        walletAccessService.requireViewWallet(contact);
        Wallet wallet = getOrCreateWallet(contact.getUserid());
        return ClientWalletResponse.from(wallet, contact, walletAccessService);
    }

    public Wallet getWalletByClientUserId(Integer clientUserId) {
        return walletRepository.findByClient_UserId(clientUserId)
                .orElseThrow(() -> BusinessException.notFound("Carteira não encontrada"));
    }

    /**
     * Valida saldo disponível antes de reservar créditos (criação de torneio, etc.).
     */
    public void requireAvailableBalance(Integer clientUserId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Wallet wallet = getOrCreateWallet(clientUserId);
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw BusinessException.badRequest(
                    "Saldo insuficiente. Necessário "
                            + amount.stripTrailingZeros().toPlainString()
                            + " créditos, disponível "
                            + wallet.getAvailableBalance().stripTrailingZeros().toPlainString()
                            + " créditos.");
        }
    }

    /**
     * Inicia a compra de créditos. Toda compra passa obrigatoriamente pelo
     * Perfex: gera uma fatura e retorna a URL de pagamento. O saldo só é
     * creditado após o pagamento da fatura (hook do módulo Perfex -> adminDeposit).
     */
    public CreditPurchaseResponse purchaseCreditsForContact(Contact contact, BigDecimal credits) {
        walletAccessService.requireUseWallet(contact);
        return perfexClient.createCreditInvoice(contact.getUserid(), contact.getId(), credits);
    }

    @Transactional
    public Transaction withdrawForContact(Contact contact, BigDecimal amount, String description) {
        walletAccessService.requireUseWallet(contact);
        return withdraw(contact.getUserid(), contact, amount, description);
    }

    @Transactional
    public Transaction holdCredits(Integer clientUserId, Contact performedBy, BigDecimal amount,
                                   String referenceType, Long referenceId) {
        if (performedBy != null) {
            walletAccessService.requireUseWallet(performedBy);
            if (!clientUserId.equals(performedBy.getUserid())) {
                throw BusinessException.forbidden("Créditos pertencem a outro cliente");
            }
        }

        Wallet wallet = getWalletForUpdate(clientUserId);
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw BusinessException.badRequest("Saldo insuficiente para reserva");
        }

        wallet.setHeldBalance(wallet.getHeldBalance().add(amount));
        walletRepository.save(wallet);

        Transaction tx = Transaction.builder()
                .wallet(wallet)
                .performedBy(performedBy)
                .amount(amount.negate())
                .type(TransactionType.HOLD)
                .status(TransactionStatus.HELD)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .description("Reserva de créditos")
                .balanceBefore(wallet.getBalance())
                .balanceAfter(wallet.getBalance())
                .build();

        return transactionRepository.save(tx);
    }

    @Transactional
    public void captureHold(Integer clientUserId, String referenceType, Long referenceId) {
        Wallet wallet = getWalletForUpdate(clientUserId);
        var holdTxs = transactionRepository.findByWalletIdAndReferenceTypeAndReferenceIdAndStatus(
                wallet.getId(), referenceType, referenceId, TransactionStatus.HELD);

        for (Transaction holdTx : holdTxs) {
            BigDecimal amount = holdTx.getAmount().negate();
            BigDecimal balanceBefore = wallet.getBalance();

            wallet.setBalance(balanceBefore.subtract(amount));
            wallet.setHeldBalance(wallet.getHeldBalance().subtract(amount));

            holdTx.setStatus(TransactionStatus.COMPLETED);
            transactionRepository.save(holdTx);

            Transaction captureTx = Transaction.builder()
                    .wallet(wallet)
                    .performedBy(holdTx.getPerformedBy())
                    .amount(amount.negate())
                    .type(TransactionType.HOLD_CAPTURE)
                    .status(TransactionStatus.COMPLETED)
                    .referenceType(referenceType)
                    .referenceId(referenceId)
                    .description("Captura de reserva")
                    .balanceBefore(balanceBefore)
                    .balanceAfter(wallet.getBalance())
                    .build();
            transactionRepository.save(captureTx);
        }

        walletRepository.save(wallet);
    }

    @Transactional
    public void releaseHold(Integer clientUserId, String referenceType, Long referenceId) {
        Wallet wallet = getWalletForUpdate(clientUserId);
        var holdTxs = transactionRepository.findByWalletIdAndReferenceTypeAndReferenceIdAndStatus(
                wallet.getId(), referenceType, referenceId, TransactionStatus.HELD);

        for (Transaction holdTx : holdTxs) {
            BigDecimal amount = holdTx.getAmount().negate();
            wallet.setHeldBalance(wallet.getHeldBalance().subtract(amount));

            holdTx.setStatus(TransactionStatus.RELEASED);
            transactionRepository.save(holdTx);

            Transaction releaseTx = Transaction.builder()
                    .wallet(wallet)
                    .performedBy(holdTx.getPerformedBy())
                    .amount(amount)
                    .type(TransactionType.HOLD_RELEASE)
                    .status(TransactionStatus.COMPLETED)
                    .referenceType(referenceType)
                    .referenceId(referenceId)
                    .description("Liberação de reserva")
                    .balanceBefore(wallet.getBalance())
                    .balanceAfter(wallet.getBalance())
                    .build();
            transactionRepository.save(releaseTx);
        }

        walletRepository.save(wallet);
    }

    @Transactional
    public void linkHoldReference(Integer clientUserId, String referenceType, Long fromReferenceId, Long toReferenceId) {
        Wallet wallet = getWalletForUpdate(clientUserId);
        var completedTxs = transactionRepository.findByWalletIdAndReferenceTypeAndReferenceIdAndStatus(
                wallet.getId(), referenceType, fromReferenceId, TransactionStatus.COMPLETED);
        for (Transaction tx : completedTxs) {
            tx.setReferenceId(toReferenceId);
            transactionRepository.save(tx);
        }

        var heldTxs = transactionRepository.findByWalletIdAndReferenceTypeAndReferenceIdAndStatus(
                wallet.getId(), referenceType, fromReferenceId, TransactionStatus.HELD);
        for (Transaction tx : heldTxs) {
            tx.setReferenceId(toReferenceId);
            transactionRepository.save(tx);
        }
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsForContact(Contact contact, Pageable pageable) {
        walletAccessService.requireViewWallet(contact);
        Wallet wallet = getOrCreateWallet(contact.getUserid());
        return transactionRepository.findByWalletIdWithPerformedBy(wallet.getId(), pageable)
                .map(TransactionResponse::from);
    }

    public AdminClientWalletResponse getAdminClientWallet(Integer clientUserId) {
        Client client = clientRepository.findById(clientUserId)
                .orElseThrow(() -> BusinessException.notFound("Cliente não encontrado"));
        Wallet wallet = walletRepository.findByClient_UserId(clientUserId).orElse(null);
        return AdminClientWalletResponse.from(client, wallet);
    }

    public Page<AdminWalletTransactionResponse> getClientTransactions(Integer clientUserId, Pageable pageable) {
        return transactionRepository.findByClientUserId(clientUserId, pageable)
                .map(AdminWalletTransactionResponse::from);
    }

    @Transactional
    public Transaction adminDeposit(Integer clientUserId, BigDecimal amount, String description) {
        Client client = clientRepository.findById(clientUserId)
                .orElseThrow(() -> BusinessException.notFound("Cliente não encontrado"));
        ensureWalletExists(client);
        String finalDescription = normalizeStaffDescription(description, "Créditos adicionados pelo staff");
        Transaction tx = deposit(clientUserId, null, amount, finalDescription);
        auditService.recordStaffMessage("DEPOSIT", "wallet", clientUserId.longValue(),
                "Créditos adicionados: " + amount);
        return tx;
    }

    @Transactional
    public Transaction adminWithdraw(Integer clientUserId, BigDecimal amount, String description) {
        clientRepository.findById(clientUserId)
                .orElseThrow(() -> BusinessException.notFound("Cliente não encontrado"));
        String finalDescription = normalizeStaffDescription(description, "Créditos removidos pelo staff");
        Transaction tx = withdraw(clientUserId, null, amount, finalDescription);
        auditService.recordStaffMessage("WITHDRAW", "wallet", clientUserId.longValue(),
                "Créditos removidos: " + amount);
        return tx;
    }

    @Transactional
    public Transaction deposit(Integer clientUserId, Contact performedBy, BigDecimal amount, String description) {
        Wallet wallet = getWalletForUpdate(clientUserId);
        BigDecimal balanceBefore = wallet.getBalance();
        wallet.setBalance(balanceBefore.add(amount));
        walletRepository.save(wallet);

        Transaction tx = Transaction.builder()
                .wallet(wallet)
                .performedBy(performedBy)
                .amount(amount)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .description(description)
                .balanceBefore(balanceBefore)
                .balanceAfter(wallet.getBalance())
                .build();

        return transactionRepository.save(tx);
    }

    @Transactional
    public Transaction withdraw(Integer clientUserId, Contact performedBy, BigDecimal amount, String description) {
        Wallet wallet = getWalletForUpdate(clientUserId);
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw BusinessException.badRequest("Saldo insuficiente");
        }

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.setBalance(balanceBefore.subtract(amount));
        walletRepository.save(wallet);

        Transaction tx = Transaction.builder()
                .wallet(wallet)
                .performedBy(performedBy)
                .amount(amount.negate())
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.COMPLETED)
                .description(description)
                .balanceBefore(balanceBefore)
                .balanceAfter(wallet.getBalance())
                .build();

        return transactionRepository.save(tx);
    }

    private Wallet getOrCreateWallet(Integer clientUserId) {
        return walletRepository.findByClient_UserId(clientUserId)
                .orElseGet(() -> {
                    Client client = clientRepository.findById(clientUserId)
                            .orElseThrow(() -> BusinessException.notFound("Cliente não encontrado"));
                    return ensureWalletExists(client);
                });
    }

    private Wallet getWalletForUpdate(Integer clientUserId) {
        return walletRepository.findByClientUserIdForUpdate(clientUserId)
                .orElseGet(() -> {
                    Client client = clientRepository.findById(clientUserId)
                            .orElseThrow(() -> BusinessException.notFound("Cliente não encontrado"));
                    ensureWalletExists(client);
                    return walletRepository.findByClientUserIdForUpdate(clientUserId)
                            .orElseThrow(() -> BusinessException.notFound("Carteira não encontrada"));
                });
    }

    private Wallet ensureWalletExists(Client client) {
        return walletRepository.findByClient_UserId(client.getUserId())
                .orElseGet(() -> walletRepository.save(Wallet.builder().client(client).build()));
    }

    private String normalizeStaffDescription(String description, String fallback) {
        if (description == null || description.isBlank()) {
            return fallback;
        }
        return description.trim();
    }
}
