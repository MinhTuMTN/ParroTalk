package com.parrotalk.backend.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.parrotalk.backend.entity.PasswordResetToken;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Soft-deletes every active (non-used, non-deleted) token that belongs to
     * the given user, ensuring only the newest token remains valid.
     */
    @Modifying
    @Query("""
            UPDATE PasswordResetToken t
               SET t.isDeleted = true,
                   t.updatedAt = CURRENT_TIMESTAMP
             WHERE t.user.id = :userId
               AND t.usedAt IS NULL
               AND t.isDeleted = false
            """)
    int invalidateAllActiveTokensForUser(@Param("userId") UUID userId);

    /**
     * Hard-deletes tokens that are both expired and used (or soft-deleted),
     * older than the given cutoff. Used by the scheduled cleanup job.
     */
    @Modifying
    @Query("""
            DELETE FROM PasswordResetToken t
             WHERE t.expiredAt < :cutoff
            """)
    int deleteExpiredTokensBefore(@Param("cutoff") LocalDateTime cutoff);
}
