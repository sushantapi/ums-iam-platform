package com.ums.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.ums.entity.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

	List<AuditLog> findByUserId(String userId);

	
	List<AuditLog> findByEventType(String eventType);

	long countByCreatedAtGreaterThanEqual(LocalDateTime since);

	long countByEventTypeAndCreatedAtGreaterThanEqual(String eventType, LocalDateTime since);
}
