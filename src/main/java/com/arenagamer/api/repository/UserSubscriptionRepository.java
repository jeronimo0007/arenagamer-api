package com.arenagamer.api.repository;

import com.arenagamer.api.entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    Optional<UserSubscription> findByUserIdAndActiveTrue(Long userId);
}
