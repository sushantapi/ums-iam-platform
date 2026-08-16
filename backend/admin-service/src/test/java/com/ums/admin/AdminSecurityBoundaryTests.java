package com.ums.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ums.admin.config.SecurityConfig;
import com.ums.admin.controller.AdminAuditController;
import com.ums.admin.controller.AdminRoleController;
import com.ums.admin.controller.AdminUserController;
import com.ums.admin.controller.DashboardController;
import com.ums.admin.dto.response.AuditLogPageResponse;
import com.ums.admin.dto.response.DashboardResponse;
import com.ums.admin.dto.response.UserSummaryPageResponse;
import com.ums.admin.security.InternalServiceAuthenticationFilter;
import com.ums.admin.security.TrustedGatewayAuthenticationFilter;
import com.ums.admin.service.AdminAuditService;
import com.ums.admin.service.AdminOrganizationService;
import com.ums.admin.service.AdminRoleService;
import com.ums.admin.service.AdminUserService;
import com.ums.admin.service.DashboardService;

@WebMvcTest(
		controllers = {
				AdminUserController.class,
				AdminRoleController.class,
				AdminAuditController.class,
				DashboardController.class
		},
		properties = {
				"spring.cloud.config.enabled=false",
				"eureka.client.enabled=false",
				"internal.gateway.secret=test-gateway-secret",
				"internal.service.secret=test-internal-service-secret"
		})
@Import({ SecurityConfig.class, TrustedGatewayAuthenticationFilter.class, InternalServiceAuthenticationFilter.class })
class AdminSecurityBoundaryTests {

	private static final String GATEWAY_SECRET_HEADER = "X-Internal-Gateway-Secret";
	private static final String INTERNAL_SERVICE_SECRET_HEADER = "X-Internal-Service-Secret";
	private static final String AUTHENTICATED_USER_HEADER = "X-Authenticated-User";
	private static final String USER_ROLES_HEADER = "X-User-Roles";
	private static final String USER_PERMISSIONS_HEADER = "X-User-Permissions";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean private AdminUserService adminUserService;
	@MockitoBean private AdminRoleService adminRoleService;
	@MockitoBean private AdminOrganizationService adminOrganizationService;
	@MockitoBean private AdminAuditService adminAuditService;
	@MockitoBean private DashboardService dashboardService;

	@Test
	void directJwtAndSpoofedIdentityAreRejected() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users")
				.header("Authorization", "Bearer direct-token"))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/admin/users")
				.header(AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString())
				.header(USER_ROLES_HEADER, "SUPER_ADMIN"))
				.andExpect(status().isForbidden());
	}

	@Test
	void trustedUserDirectoryAccessIsAcceptedAndBounded() throws Exception {
		when(adminUserService.getUsers(0, 20, null))
				.thenReturn(new UserSummaryPageResponse(List.of(), 0, 20, 0, 0));

		mockMvc.perform(trustedGet("/api/v1/admin/users", "USER_ADMIN"))
				.andExpect(status().isOk());
		mockMvc.perform(trustedGet("/api/v1/admin/users", "ORG_ADMIN"))
				.andExpect(status().isForbidden());
		mockMvc.perform(trustedGet("/api/v1/admin/users", "USER_ADMIN").param("size", "201"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(trustedGet("/api/v1/admin/users", "USER_ADMIN").param("status", "ACTIVE"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void supportCanReadUsersAndDashboardButNotAuditOrRoleMutation() throws Exception {
		when(adminUserService.getUsers(0, 20, null))
				.thenReturn(new UserSummaryPageResponse(List.of(), 0, 20, 0, 0));
		when(dashboardService.getDashboardSummary()).thenReturn(new DashboardResponse(
				new DashboardResponse.UserMetrics(0, 0, 0, 0),
				new DashboardResponse.OrganizationMetrics(0, 0, 0),
				new DashboardResponse.RoleMetrics(0),
				new DashboardResponse.AuditMetrics(0, 0)));

		mockMvc.perform(trustedGet("/api/v1/admin/users", "SUPPORT")).andExpect(status().isOk());
		mockMvc.perform(trustedGet("/api/v1/admin/dashboard", "SUPPORT")).andExpect(status().isOk());
		mockMvc.perform(trustedGet("/api/v1/admin/audit/logs", "SUPPORT")).andExpect(status().isForbidden());
		mockMvc.perform(trustedRoleAssignment("SUPPORT")).andExpect(status().isForbidden());
	}

	@Test
	void auditAndRoleFamiliesRequireTheirSpecificAuthorities() throws Exception {
		when(adminAuditService.getAuditLogs(0, 50, null, null, null, null))
				.thenReturn(new AuditLogPageResponse(List.of(), 0, 50, 0, 0));
		when(adminRoleService.assignRole(any())).thenReturn("Role assigned successfully");

		mockMvc.perform(trustedGet("/api/v1/admin/audit/logs", "AUDIT_ADMIN"))
				.andExpect(status().isOk());
		mockMvc.perform(trustedGetWithPermission("/api/v1/admin/audit/logs", "AUDIT_READ"))
				.andExpect(status().isOk());
		mockMvc.perform(trustedRoleAssignment("AUTH_ADMIN")).andExpect(status().isOk());
		verify(adminRoleService).assignRole(any());
	}

	@Test
	void authAdminCannotEscalateToPrivilegedPlatformRole() throws Exception {
		mockMvc.perform(post("/api/v1/admin/roles/assign")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"userId":"00000000-0000-0000-0000-000000000002","roleName":"SUPER_ADMIN"}
						""")
				.header(AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString())
				.header(GATEWAY_SECRET_HEADER, "test-gateway-secret")
				.header(USER_ROLES_HEADER, "AUTH_ADMIN"))
				.andExpect(status().isForbidden());
	}

	@Test
	void internalSecretCannotAccessExternalRoutesAndOnlyAuthenticatesInternalNamespace() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users")
				.header(INTERNAL_SERVICE_SECRET_HEADER, "test-internal-service-secret"))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/internal/admin/ping")
				.header(GATEWAY_SECRET_HEADER, "test-gateway-secret"))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/internal/admin/ping")
				.header(INTERNAL_SERVICE_SECRET_HEADER, "test-internal-service-secret"))
				.andExpect(status().isNotFound());
	}

	@Test
	void swaggerIsNotPublic() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isForbidden());
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder trustedGet(
			String path, String role) {
		return get(path)
				.header(AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString())
				.header(GATEWAY_SECRET_HEADER, "test-gateway-secret")
				.header(USER_ROLES_HEADER, role);
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder trustedGetWithPermission(
			String path, String permission) {
		return get(path)
				.header(AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString())
				.header(GATEWAY_SECRET_HEADER, "test-gateway-secret")
				.header(USER_PERMISSIONS_HEADER, permission);
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder trustedRoleAssignment(
			String role) {
		return post("/api/v1/admin/roles/assign")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"userId":"00000000-0000-0000-0000-000000000002","roleName":"EMPLOYEE"}
						""")
				.header(AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString())
				.header(GATEWAY_SECRET_HEADER, "test-gateway-secret")
				.header(USER_ROLES_HEADER, role);
	}
}
