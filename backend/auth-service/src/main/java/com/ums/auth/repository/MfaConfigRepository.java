package com.ums.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ums.auth.entity.MfaConfig;

public interface MfaConfigRepository extends JpaRepository<MfaConfig, UUID> {
	Optional<MfaConfig> findByUserId(UUID userId);

}
