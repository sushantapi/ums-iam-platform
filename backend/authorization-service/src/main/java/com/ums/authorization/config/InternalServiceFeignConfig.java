package com.ums.authorization.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import feign.RequestInterceptor;

public class InternalServiceFeignConfig {

	private static final String INTERNAL_SERVICE_SECRET_HEADER = "X-Internal-Service-Secret";

	@Bean
	RequestInterceptor internalServiceRequestInterceptor(
			@Value("${internal.service.secret}") String internalServiceSecret) {
		return template -> template.header(INTERNAL_SERVICE_SECRET_HEADER, internalServiceSecret);
	}
}
