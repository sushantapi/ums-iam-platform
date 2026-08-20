package com.ums.hrms.leave.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.RequestInterceptor;

@Configuration
public class InternalServiceFeignConfig {

    private static final String INTERNAL_SERVICE_SECRET_HEADER = "X-Internal-Service-Secret";

    @Bean
    RequestInterceptor internalServiceSecretInterceptor(
            @Value("${internal.service.secret}") String secret) {
        return template -> template.header(INTERNAL_SERVICE_SECRET_HEADER, secret);
    }
}
