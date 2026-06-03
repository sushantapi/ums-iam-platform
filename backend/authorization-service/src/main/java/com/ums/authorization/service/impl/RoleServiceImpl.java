package com.ums.authorization.service.impl;

import com.ums.authorization.entity.Role;
import com.ums.authorization.repository.RoleRepository;
import com.ums.authorization.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

	private final RoleRepository roleRepository;

	@Override
	public Role createRole(Role role) {

		return roleRepository.save(role);
	}

	@Override
	public List<Role> getAllRoles() {

		return roleRepository.findAll();
	}

	@Override
	public Role getRoleByName(String roleName) {

		return roleRepository.findByName(roleName).orElseThrow(() -> new RuntimeException("Role not found"));
	}
}