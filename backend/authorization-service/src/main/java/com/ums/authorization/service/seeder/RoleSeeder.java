package com.ums.authorization.service.seeder;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ums.authorization.entity.Role;
import com.ums.authorization.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleSeeder {

	private static final List<String> SYSTEM_ROLES = List.of(
			"SUPER_ADMIN",
			"ORG_ADMIN",
			"USER_ADMIN",
			"AUTH_ADMIN",
			"AUDIT_ADMIN",
			"SUPPORT",
			"SECURITY",
			"COMPLIANCE",
			"NOTIFICATION_ADMIN",
			"HR_MANAGER",
			"PAYROLL_ADMIN",
			"PAYROLL_MANAGER",
			"EMPLOYEE");

	private final RoleRepository roleRepository;

	public void seed() {
		SYSTEM_ROLES.forEach(this::createRole);
	}

	private void createRole(String name) {
		if (!roleRepository.existsByNameIgnoreCase(name)) {
			roleRepository.save(Role.builder().name(name).description(name.replace('_', ' '))
					.isSystem(true).active(true).build());
		}
	}
}
