package com.ums.authorization.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ums.authorization.entity.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

	List<UserRole> findByUserId(UUID userId);

	boolean existsByUserIdAndRole_Id(UUID userId, UUID roleId);

	boolean existsByUserIdAndRole_IdAndScopeTypeAndScopeIdAndActiveTrue(UUID userId, UUID roleId, String scopeType,
			String scopeId);

	/*
	 * boolean existsByUserIdAndRole_IdAndScopeTypeAndScopeIdAndActiveTrue(UUID
	 * userId, UUID roleId, String scopeType, String scopeId);
	 * 
	 * @Query(""" select ur from UserRole ur join fetch ur.role r where ur.userId =
	 * :userId and ur.active = true and r.active = true and (ur.expiresAt is null or
	 * ur.expiresAt > CURRENT_TIMESTAMP) and ( (ur.scopeType = 'PLATFORM' and
	 * ur.scopeId = '*') or (ur.scopeType = :scopeType and ur.scopeId = :scopeId) )
	 * """) List<UserRole> findActiveAssignments(@Param("userId") UUID
	 * userId, @Param("scopeType") String scopeType,
	 * 
	 * @Param("scopeId") String scopeId);
	 */
}
