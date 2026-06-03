package com.ums.authorization.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ums.authorization.entity.Role;
import com.ums.authorization.repository.RoleRepository;

@Configuration
public class DataInitializer {

	@Bean
	CommandLineRunner initRoles(RoleRepository roleRepository) {

		return args -> {

			if (roleRepository.findByName("ROLE_USER").isEmpty()) {

				roleRepository.save(

						Role.builder()

								.name("ROLE_USER")

								.description("Default user role")

								.build());
			}

			if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {

				roleRepository.save(

						Role.builder()

								.name("ROLE_ADMIN")

								.description("Administrator role")

								.build());
			}
		};
	}
}