package com.ums.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ums.entity.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

	List<AuditLog> findByUserId(String userId);

	
	List<AuditLog> findByEventType(String eventType);
}