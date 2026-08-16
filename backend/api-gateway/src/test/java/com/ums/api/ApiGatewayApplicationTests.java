package com.ums.api;

import com.ums.gateway.ApiGatewayApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
		classes = ApiGatewayApplication.class,
		properties = {
				"spring.cloud.config.enabled=false",
				"eureka.client.enabled=false",
				"jwt.public-key-path=keys/public.pem",
				"internal.gateway.secret=test-gateway-secret"
		})
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
