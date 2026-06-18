package com.ums.authorization.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ums.authorization.entity.Permission;

/*
 * public interface PermissionRepository extends JpaRepository<Permission, UUID>
 * {
 * 
 * Optional<Permission> findByCodeIgnoreCase(String code);
 * 
 * Optional<Permission> findByResource_CodeIgnoreCaseAndActionIgnoreCase(String
 * resourceCode, String action);
 * 
 * boolean existsByResource_IdAndActionIgnoreCase(UUID resourceId, String
 * action);
 * 
 * }
 */

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

	Optional<Permission> findByCodeIgnoreCase(String code);

	boolean existsByCodeIgnoreCase(String code);

	Optional<Permission> findByResource_CodeIgnoreCaseAndActionIgnoreCase(String resourceCode, String action);

	boolean existsByResource_IdAndActionIgnoreCase(UUID resourceId, String action);

	List<Permission> findByResource_Id(UUID resourceId);
}