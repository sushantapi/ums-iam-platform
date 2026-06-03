package com.ums.auth.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ums.auth.entity.Role;
import com.ums.auth.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

	private final RoleRepository roleRepository;

	@Bean
	CommandLineRunner initRoles() {

		return args -> {

			createRole("SUPER_ADMIN");
			createRole("ORG_ADMIN");
			createRole("HR_MANAGER");
			createRole("EMPLOYEE");
		};
	}

	private void createRole(String roleName) {

		roleRepository.findByName(roleName).orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
	}
}
