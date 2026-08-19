package com.ums.hrms.leave.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import feign.RequestInterceptor;
@Configuration public class InternalServiceFeignConfig {
 @Bean RequestInterceptor internalServiceSecretInterceptor(@Value("${internal.service.secret}") String secret){return t->t.header("X-Internal-Service-Secret",secret);}
}
