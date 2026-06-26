package com.arenagamer.api.repository;

import com.arenagamer.api.entity.Transaction;
import com.arenagamer.api.entity.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    @Query("""
            SELECT t FROM Transaction t
            LEFT JOIN FETCH t.performedBy pb
            WHERE t.wallet.id = :walletId
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByWalletIdWithPerformedBy(@Param("walletId") Long walletId, Pageable pageable);
    List<Transaction> findByReferenceTypeAndReferenceIdAndStatus(String referenceType, Long referenceId, TransactionStatus status);
    List<Transaction> findByWalletIdAndReferenceTypeAndReferenceIdAndStatus(Long walletId, String referenceType, Long referenceId, TransactionStatus status);

    @Query("""
            SELECT t FROM Transaction t
            JOIN FETCH t.wallet w
            JOIN FETCH w.client c
            LEFT JOIN FETCH t.performedBy pb
            WHERE c.userId = :clientUserId
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByClientUserId(@Param("clientUserId") Integer clientUserId, Pageable pageable);
}
