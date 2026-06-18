package com.ums.authorization.service.seeder;

import org.springframework.stereotype.Service;

import com.ums.authorization.entity.Role;
import com.ums.authorization.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleSeeder {

	private final RoleRepository roleRepository;

	public void seed() {

		createRole("SUPER_ADMIN");
		createRole("ORG_ADMIN");
		createRole("HR_MANAGER");
		createRole("PAYROLL_MANAGER");
		createRole("EMPLOYEE");
	}

	private void createRole(String name) {

		if (!roleRepository.existsByNameIgnoreCase(name)) {

			roleRepository.save(Role.builder().name(name).build());
		}
	}
}