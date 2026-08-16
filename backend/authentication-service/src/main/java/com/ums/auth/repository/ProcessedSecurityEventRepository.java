package com.ums.auth.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ums.auth.entity.ProcessedSecurityEvent;

public interface ProcessedSecurityEventRepository extends JpaRepository<ProcessedSecurityEvent, UUID> {
}
