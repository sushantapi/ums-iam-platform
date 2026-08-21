package com.ums.auth.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ums.auth.entity.PasswordResetToken;

import jakarta.persistence.LockModeType;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from PasswordResetToken t join fetch t.user where t.tokenHash = :tokenHash")
	Optional<PasswordResetToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update PasswordResetToken t
			set t.revokedAt = :revokedAt
			where t.user.id = :userId
			  and t.consumedAt is null
			  and t.revokedAt is null
			  and t.expiresAt > :now
			""")
	int revokeActiveTokens(
			@Param("userId") UUID userId,
			@Param("now") Instant now,
			@Param("revokedAt") Instant revokedAt);
}
