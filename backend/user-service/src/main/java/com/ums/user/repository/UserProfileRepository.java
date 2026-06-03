package com.ums.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ums.user.entity.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

	Optional<UserProfile> findByEmail(String email);
}