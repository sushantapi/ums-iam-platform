package com.ums.authorization;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ums.authorization.repository.PermissionRepository;
import com.ums.authorization.repository.ResourceRepository;
import com.ums.authorization.repository.RolePermissionRepository;
import com.ums.authorization.repository.RoleRepository;
import com.ums.authorization.service.AuthorizationService;

@SpringBootTest(properties = {
		"spring.cloud.config.enabled=false",
		"eureka.client.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:authorization_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.flyway.enabled=false",
		"management.health.rabbit.enabled=false",
		"internal.gateway.secret=test-gateway-secret",
		"internal.service.secret=test-internal-service-secret"
})
@AutoConfigureMockMvc
class AuthorizationServiceApplicationTests {

	private static final List<String> LEAVE_PERMISSIONS = List.of(
			"LEAVE_READ",
			"LEAVE_REQUEST_CREATE",
			"LEAVE_APPROVE",
			"LEAVE_CANCEL");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ResourceRepository resourceRepository;

	@Autowired
	private PermissionRepository permissionRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private RolePermissionRepository rolePermissionRepository;

	@MockitoBean
	private AuthorizationService authorizationService;

	@Test
	void contextLoads() {
	}

	@Test
	void leaveResourceAndPermissionsAreSeeded() {
		assertTrue(resourceRepository.existsByCodeIgnoreCase("LEAVE"));
		LEAVE_PERMISSIONS.forEach(permissionCode -> assertTrue(
				permissionRepository.existsByCodeIgnoreCase(permissionCode),
				() -> "Missing leave permission: " + permissionCode));
	}

	@Test
	void hrManagerReceivesAllLeavePermissions() {
		assertRoleHasAllLeavePermissions("HR_MANAGER");
	}

	@Test
	void superAdminAutomaticallyReceivesAllLeavePermissions() {
		assertRoleHasAllLeavePermissions("SUPER_ADMIN");
	}

	@Test
	void internalRoleAssignmentRequiresInternalServiceSecret() throws Exception {
		when(authorizationService.assignRole(any())).thenReturn("Role assigned successfully");
		String body = """
				{"userId":"00000000-0000-0000-0000-000000000002",
				 "roleName":"EMPLOYEE","scopeType":"PLATFORM","scopeId":"*",
				 "assignedBy":"00000000-0000-0000-0000-000000000001"}
				""";

		mockMvc.perform(post("/api/v1/internal/roles/assign")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/internal/roles/assign")
				.header("X-Internal-Service-Secret", "test-internal-service-secret")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isOk());
	}

	@Test
	void externalRouteRejectsDirectJwtAndSpoofedGatewayIdentity() throws Exception {
		mockMvc.perform(get("/api/v1/roles")
				.header("Authorization", "Bearer not-a-real-token"))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/roles")
				.header("X-Authenticated-User", "00000000-0000-0000-0000-000000000001")
				.header("X-User-Roles", "SUPER_ADMIN"))
				.andExpect(status().isForbidden());
	}

	@Test
	void trustedGatewayIdentityCanAccessAuthorizedExternalRoute() throws Exception {
		mockMvc.perform(get("/api/v1/roles")
				.header("X-Authenticated-User", "00000000-0000-0000-0000-000000000001")
				.header("X-Internal-Gateway-Secret", "test-gateway-secret")
				.header("X-User-Roles", "SUPER_ADMIN"))
				.andExpect(status().isOk());
	}

	@Test
	void internalRouteRejectsGatewaySecretAndDirectJwt() throws Exception {
		mockMvc.perform(post("/api/v1/internal/roles/assign")
				.header("X-Internal-Gateway-Secret", "test-gateway-secret")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/internal/roles/assign")
				.header("Authorization", "Bearer not-a-real-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void healthAndInfoArePublicWhileTestAndSwaggerRoutesAreClosed() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
		mockMvc.perform(get("/api/test/role")).andExpect(status().isForbidden());
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isForbidden());
	}

	private void assertRoleHasAllLeavePermissions(String roleName) {
		var role = roleRepository.findByNameIgnoreCase(roleName)
				.orElseThrow(() -> new AssertionError("Missing role: " + roleName));

		LEAVE_PERMISSIONS.forEach(permissionCode -> {
			var permission = permissionRepository.findByCodeIgnoreCase(permissionCode)
					.orElseThrow(() -> new AssertionError("Missing permission: " + permissionCode));
			assertTrue(
					rolePermissionRepository.existsByRole_IdAndPermission_Id(role.getId(), permission.getId()),
					() -> roleName + " missing permission: " + permissionCode);
		});
	}

}
