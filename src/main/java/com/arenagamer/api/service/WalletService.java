package com.arenagamer.api.service;

import com.arenagamer.api.entity.Transaction;
import com.arenagamer.api.entity.Wallet;
import com.arenagamer.api.entity.enums.TransactionStatus;
import com.arenagamer.api.entity.enums.TransactionType;
import com.arenagamer.api.exception.BusinessException;
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

    public Wallet getWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> BusinessException.notFound("Carteira não encontrada"));
    }

    @Transactional
    public Transaction deposit(Long userId, BigDecimal amount, String description) {
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> BusinessException.notFound("Carteira não encontrada"));

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.setBalance(balanceBefore.add(amount));
        walletRepository.save(wallet);

        Transaction tx = Transaction.builder()
                .wallet(wallet)
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
    public Transaction withdraw(Long userId, BigDecimal amount, String description) {
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> BusinessException.notFound("Carteira não encontrada"));

        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw BusinessException.badRequest("Saldo insuficiente");
        }

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.setBalance(balanceBefore.subtract(amount));
        walletRepository.save(wallet);

        Transaction tx = Transaction.builder()
                .wallet(wallet)
                .amount(amount.negate())
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.COMPLETED)
                .description(description)
                .balanceBefore(balanceBefore)
                .balanceAfter(wallet.getBalance())
                .build();

        return transactionRepository.save(tx);
    }

    @Transactional
    public Transaction holdCredits(Long userId, BigDecimal amount, String referenceType, Long referenceId) {
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> BusinessException.notFound("Carteira não encontrada"));

        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw BusinessException.badRequest("Saldo insuficiente para reserva");
        }

        wallet.setHeldBalance(wallet.getHeldBalance().add(amount));
        walletRepository.save(wallet);

        Transaction tx = Transaction.builder()
                .wallet(wallet)
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
    public void captureHold(Long userId, String referenceType, Long referenceId) {
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> BusinessException.notFound("Carteira não encontrada"));

        var holdTxs = transactionRepository.findByReferenceTypeAndReferenceIdAndStatus(
                referenceType, referenceId, TransactionStatus.HELD);

        for (Transaction holdTx : holdTxs) {
            BigDecimal amount = holdTx.getAmount().negate();
            BigDecimal balanceBefore = wallet.getBalance();

            wallet.setBalance(balanceBefore.subtract(amount));
            wallet.setHeldBalance(wallet.getHeldBalance().subtract(amount));

            holdTx.setStatus(TransactionStatus.COMPLETED);
            transactionRepository.save(holdTx);

            Transaction captureTx = Transaction.builder()
                    .wallet(wallet)
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
    public void releaseHold(Long userId, String referenceType, Long referenceId) {
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> BusinessException.notFound("Carteira não encontrada"));

        var holdTxs = transactionRepository.findByReferenceTypeAndReferenceIdAndStatus(
                referenceType, referenceId, TransactionStatus.HELD);

        for (Transaction holdTx : holdTxs) {
            BigDecimal amount = holdTx.getAmount().negate();
            wallet.setHeldBalance(wallet.getHeldBalance().subtract(amount));

            holdTx.setStatus(TransactionStatus.RELEASED);
            transactionRepository.save(holdTx);

            Transaction releaseTx = Transaction.builder()
                    .wallet(wallet)
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

    public Page<Transaction> getTransactions(Long userId, Pageable pageable) {
        Wallet wallet = getWallet(userId);
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable);
    }
}
