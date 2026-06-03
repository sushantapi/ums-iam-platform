package com.ums.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ums.auth.entity.Session;

public interface SessionRepository extends JpaRepository<Session, UUID> {

	Optional<Session> findByRefreshTokenHash(String refreshTokenHash);

	List<Session> findByUserId(UUID userId);
}