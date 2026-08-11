package com.ums.org.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.RequestInterceptor;

@Configuration
public class InternalServiceFeignConfig {

	private static final String INTERNAL_SERVICE_SECRET_HEADER = "X-Internal-Service-Secret";

	@Bean
	RequestInterceptor internalServiceRequestInterceptor(
			@Value("${internal.service.secret}") String internalServiceSecret) {
		return template -> template.header(INTERNAL_SERVICE_SECRET_HEADER, internalServiceSecret);
	}
}
