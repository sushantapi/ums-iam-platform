package com.ums.auth.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ums.auth.entity.User;
import com.ums.auth.entity.User.UserStatus;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	long countByStatus(UserStatus status);

	@Query("select count(u) from User u where u.lockedUntil is not null and u.lockedUntil > :now")
	long countLockedUsers(@Param("now") Instant now);

	@Modifying
	@Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :id")
	void incrementFailedAttempts(UUID id);

	@Modifying
	@Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lockedUntil = null WHERE u.id = :id")
	void resetFailedAttempts(UUID id);
	
	
	
}