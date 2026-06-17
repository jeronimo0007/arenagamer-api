package com.arenagamer.api.repository;

import com.arenagamer.api.entity.Transaction;
import com.arenagamer.api.entity.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);
    List<Transaction> findByReferenceTypeAndReferenceIdAndStatus(String referenceType, Long referenceId, TransactionStatus status);
    List<Transaction> findByWalletIdAndReferenceTypeAndReferenceIdAndStatus(Long walletId, String referenceType, Long referenceId, TransactionStatus status);
}
