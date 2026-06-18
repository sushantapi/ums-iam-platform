package com.ums.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ums.entity.AuditLog;

public interface AuditService {

	Page<AuditLog> getAll(Pageable pageable);
}