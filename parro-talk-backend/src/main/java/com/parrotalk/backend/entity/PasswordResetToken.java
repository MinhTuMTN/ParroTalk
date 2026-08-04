package com.parrotalk.backend.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stores hashed password-reset tokens issued to users.
 *
 * <p>The raw token is sent to the user via email; only its SHA-256 hash is
 * persisted here so that a database breach does not expose valid reset links.
 *
 * @author MinhTuMTN
 */
@Entity
@Table(name = "password_reset_tokens", indexes = {
        @Index(name = "idx_prt_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_prt_user_id", columnList = "user_id"),
        @Index(name = "idx_prt_expired_at", columnList = "expired_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE password_reset_tokens SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class PasswordResetToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The user who requested the password reset. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** SHA-256 hex digest of the raw token. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /** When this token expires. */
    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    /** When this token was consumed (null if still unused). */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /** Principal that created this token (typically "system"). */
    @Column(name = "created_by", nullable = false)
    @Builder.Default
    private String createdBy = "system";

    // ---- convenience helpers ----

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiredAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }
}
