package com.ums.authorization.startup;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.ums.authorization.service.seeder.PermissionSeeder;
import com.ums.authorization.service.seeder.ResourceSeeder;
import com.ums.authorization.service.seeder.RolePermissionSeeder;
import com.ums.authorization.service.seeder.RoleSeeder;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthorizationDataSeeder implements CommandLineRunner {

	private final ResourceSeeder resourceSeeder;
	private final RoleSeeder roleSeeder;
	private final PermissionSeeder permissionSeeder;
	private final RolePermissionSeeder rolePermissionSeeder;

	@Override
	public void run(String... args) {

		resourceSeeder.seed();

		roleSeeder.seed();

		permissionSeeder.seed();

		rolePermissionSeeder.seed();
	}
}