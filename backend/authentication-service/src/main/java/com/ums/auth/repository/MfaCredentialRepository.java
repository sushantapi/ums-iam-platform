package com.ums.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ums.auth.entity.MfaCredential;

import jakarta.persistence.LockModeType;

public interface MfaCredentialRepository extends JpaRepository<MfaCredential, UUID> {

	Optional<MfaCredential> findByUserId(UUID userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select credential from MfaCredential credential where credential.userId = :userId")
	Optional<MfaCredential> findByUserIdForUpdate(@Param("userId") UUID userId);

	void deleteByUserId(UUID userId);
}
