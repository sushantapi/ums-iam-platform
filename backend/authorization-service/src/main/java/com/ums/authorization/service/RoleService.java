package com.ums.authorization.service;

import com.ums.authorization.entity.Role;

import java.util.List;

public interface RoleService {

	Role createRole(Role role);

	List<Role> getAllRoles();

	Role getRoleByName(String roleName);
}