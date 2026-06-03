package com.ums.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.RequestInterceptor;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class FeignClientInterceptor {

	private final HttpServletRequest request;

	@Bean
	public RequestInterceptor requestInterceptor() {

		return template -> {

			String authorization = request.getHeader("Authorization");

			if (authorization != null) {

				template.header("Authorization", authorization);
			}
		};
	}
}