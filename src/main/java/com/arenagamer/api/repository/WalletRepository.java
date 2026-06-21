package com.arenagamer.api.repository;

import com.arenagamer.api.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByClient_UserId(Integer clientUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.client.userId = :clientUserId")
    Optional<Wallet> findByClientUserIdForUpdate(@Param("clientUserId") Integer clientUserId);
}
