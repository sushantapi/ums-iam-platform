package com.ums.authorization.service.seeder;

import org.springframework.stereotype.Service;

import com.ums.authorization.entity.Permission;
import com.ums.authorization.entity.Resource;
import com.ums.authorization.repository.PermissionRepository;
import com.ums.authorization.repository.ResourceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionSeeder {

	private final ResourceRepository resourceRepository;
	private final PermissionRepository permissionRepository;

	public void seed() {

		createPermission("USER", "CREATE");
		createPermission("USER", "UPDATE");
		createPermission("USER", "DELETE");

		createPermission("ROLE", "CREATE");
		createPermission("ROLE", "UPDATE");
		createPermission("ROLE", "ASSIGN");

		createPermission("ORGANIZATION", "CREATE");
		createPermission("ORGANIZATION", "UPDATE");
		createPermission("ORGANIZATION", "DELETE");

		createPermission("PAYROLL", "VIEW");
		createPermission("PAYROLL", "PROCESS");

		createPermission("PROJECT", "CREATE");
		createPermission("PROJECT", "UPDATE");
		createPermission("PROJECT", "DELETE");
	}

	private void createPermission(String resourceCode, String action) {

		Resource resource = resourceRepository.findByCodeIgnoreCase(resourceCode).orElseThrow();

		String permissionCode = resourceCode + "_" + action;

		if (permissionRepository.findByCodeIgnoreCase(permissionCode).isEmpty()) {

			permissionRepository.save(Permission.builder().code(permissionCode).resource(resource).action(action)
					.description(permissionCode).build());
		}
	}
}