package com.ums.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ums.admin.client.AuditServiceClient;
import com.ums.admin.client.RoleServiceClient;
import com.ums.admin.client.UserServiceClient;

@SpringBootTest(properties = {
		"spring.cloud.config.enabled=false",
		"eureka.client.enabled=false",
		"internal.gateway.secret=test-gateway-secret",
		"internal.service.secret=test-internal-service-secret"
})
@AutoConfigureMockMvc
class AdminServiceApplicationTests {

	@MockitoBean
	private UserServiceClient userServiceClient;

	@MockitoBean
	private RoleServiceClient roleServiceClient;

	@MockitoBean
	private AuditServiceClient auditServiceClient;

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
