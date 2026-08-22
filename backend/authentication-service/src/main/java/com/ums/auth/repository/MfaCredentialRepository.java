package com.ums.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ums.auth.entity.MfaCredential;

public interface MfaCredentialRepository extends JpaRepository<MfaCredential, UUID> {

	Optional<MfaCredential> findByUserId(UUID userId);

	void deleteByUserId(UUID userId);
}
