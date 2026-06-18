package com.ums.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ums.entity.AuditLog;
import com.ums.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository repository;

    @Override
    public Page<AuditLog> getAll(Pageable pageable) {
        return repository.findAll(pageable);
    }
}