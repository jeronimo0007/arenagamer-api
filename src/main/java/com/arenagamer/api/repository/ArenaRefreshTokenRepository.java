package com.arenagamer.api.repository;

import com.arenagamer.api.entity.ArenaRefreshToken;
import com.arenagamer.api.entity.enums.AuthUserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ArenaRefreshTokenRepository extends JpaRepository<ArenaRefreshToken, Long> {
    Optional<ArenaRefreshToken> findByRefreshToken(String refreshToken);

    Optional<ArenaRefreshToken> findByUserTypeAndUserId(AuthUserType userType, Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ArenaRefreshToken t WHERE t.userType = :userType AND t.userId = :userId")
    void deleteByUserTypeAndUserId(@Param("userType") AuthUserType userType, @Param("userId") Long userId);
}