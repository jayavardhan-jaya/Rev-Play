package com.revature.revplaydemo.auth.repository;

import com.revature.revplaydemo.auth.entity.PasswordResetTokenEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    Optional<PasswordResetTokenEntity> findByToken(String token);

    long deleteByExpiresAtBeforeOrUsedAtIsNotNull(Instant expiryTime);
}
