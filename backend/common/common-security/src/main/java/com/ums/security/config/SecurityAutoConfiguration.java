package com.ums.security.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@AutoConfiguration
@EnableMethodSecurity
@ComponentScan(basePackages = "com.ums.security")
public class SecurityAutoConfiguration {

	@Bean
	public String securityLibraryLoaded() {
		return "COMMON_SECURITY_LOADED";
	}
}
