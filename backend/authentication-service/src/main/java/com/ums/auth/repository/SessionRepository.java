package com.ums.auth.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.ums.auth.entity.Session;

import jakarta.persistence.LockModeType;

public interface SessionRepository extends JpaRepository<Session, UUID>, JpaSpecificationExecutor<Session> {

	List<Session> findByUserId(UUID userId);

	List<Session> findByOrganizationIdAndRevokedFalseAndMfaVerifiedFalseAndExpiresAtAfter(
			UUID organizationId,
			Instant expiresAt);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from Session s join fetch s.user where s.id = :sessionId")
	Optional<Session> findByIdForRefresh(UUID sessionId);
}
