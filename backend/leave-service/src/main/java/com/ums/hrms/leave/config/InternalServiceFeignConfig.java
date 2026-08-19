package com.ums.hrms.leave.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.RequestInterceptor;

@Configuration
public class InternalServiceFeignConfig {

    @Bean
    RequestInterceptor internalServiceSecretInterceptor(
            @Value("${internal.service.secret}") String internalServiceSecret) {
        return requestTemplate -> requestTemplate.header("X-Internal-Service-Secret", internalServiceSecret);
    }
}
