package com.ums.auth.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ums.auth.entity.AuthSession;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

	List<AuthSession> findByUserId(UUID userId);

}
