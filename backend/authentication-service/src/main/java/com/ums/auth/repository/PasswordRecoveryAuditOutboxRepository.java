package com.ums.auth.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ums.auth.entity.PasswordRecoveryAuditOutboxEvent;
import com.ums.auth.entity.PasswordRecoveryAuditOutboxEvent.Status;

import jakarta.persistence.LockModeType;

public interface PasswordRecoveryAuditOutboxRepository extends JpaRepository<PasswordRecoveryAuditOutboxEvent, UUID> {

	List<PasswordRecoveryAuditOutboxEvent> findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
			Status status, Instant nextAttemptAt, Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select event from PasswordRecoveryAuditOutboxEvent event where event.id = :id")
	Optional<PasswordRecoveryAuditOutboxEvent> findByIdForUpdate(@Param("id") UUID id);
}
