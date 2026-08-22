package com.ums.auth.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ums.auth.entity.MfaRecoveryCode;

public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCode, UUID> {

	List<MfaRecoveryCode> findAllByCredentialId(UUID credentialId);

	long countByCredentialIdAndConsumedAtIsNull(UUID credentialId);

	void deleteAllByCredentialId(UUID credentialId);
}
