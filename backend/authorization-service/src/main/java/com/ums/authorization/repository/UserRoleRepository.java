package com.ums.authorization.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ums.authorization.entity.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

	List<UserRole> findByUserId(UUID userId);

	boolean existsByUserIdAndRole_Id(UUID userId, UUID roleId);
}