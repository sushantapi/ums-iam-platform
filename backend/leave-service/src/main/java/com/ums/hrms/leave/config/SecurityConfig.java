package com.ums.hrms.leave.config;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import jakarta.servlet.DispatcherType;
@Configuration @EnableWebSecurity @EnableMethodSecurity
public class SecurityConfig {
 @Bean SecurityFilterChain securityFilterChain(HttpSecurity http,TrustedGatewayAuthenticationFilter filter)throws Exception{return http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).httpBasic(b->b.disable()).formLogin(f->f.disable()).addFilterBefore(filter,AbstractPreAuthenticatedProcessingFilter.class).authorizeHttpRequests(a->a.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll().requestMatchers("/actuator/health","/actuator/info").permitAll().requestMatchers("/api/v1/hrms/**").authenticated().anyRequest().denyAll()).build();}
}
