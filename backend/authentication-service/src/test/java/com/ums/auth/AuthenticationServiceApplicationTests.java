package com.ums.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ums.auth.client.AuthorizationClient;
import com.ums.auth.service.JwtService;
import com.ums.events.publisher.AuditPublisher;

@SpringBootTest(properties = {
		"spring.cloud.config.enabled=false",
		"eureka.client.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:auth_context;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.flyway.enabled=false",
		"spring.rabbitmq.listener.simple.auto-startup=false",
		"management.health.rabbit.enabled=false",
		"management.health.redis.enabled=false",
		"internal.gateway.secret=test-gateway-secret",
		"internal.service.secret=test-internal-service-secret",
		"security.password-reset.reset-page-url=http://localhost:5174/reset-password"
})
@AutoConfigureMockMvc
class AuthenticationServiceApplicationTests {

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private AuthorizationClient authorizationClient;

	@MockitoBean
	private RabbitTemplate rabbitTemplate;

	@MockitoBean
	private AuditPublisher auditPublisher;

	@MockitoBean
	private StringRedisTemplate stringRedisTemplate;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void healthAndInfoRemainPublic() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
	}
}
