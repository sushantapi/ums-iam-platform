package com.ums.authorization.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ums.authorization.entity.Role;
import com.ums.authorization.entity.RolePermission;

public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

    List<RolePermission> findByRole_Id(UUID roleId);
    
    List<RolePermission> findByRole(Role role);
}