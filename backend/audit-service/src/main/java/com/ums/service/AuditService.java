package com.ums.service;

import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.Page;

import com.ums.dto.AuditEventFilter;
import com.ums.dto.AuditEventResponse;

public interface AuditService {

	Page<AuditEventResponse> getEvents(AuditEventFilter filter, Pageable pageable);

	AuditEventResponse getEvent(long eventId);
}
