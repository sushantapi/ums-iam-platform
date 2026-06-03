package com.ums.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ums.user.entity.UserPreference;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {

	Optional<UserPreference> findByUserId(UUID userId);
}