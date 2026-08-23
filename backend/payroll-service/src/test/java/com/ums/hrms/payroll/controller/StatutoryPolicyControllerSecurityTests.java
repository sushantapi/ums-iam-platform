package com.ums.hrms.payroll.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ums.hrms.payroll.config.SecurityConfig;
import com.ums.hrms.payroll.config.TrustedGatewayAuthenticationFilter;
import com.ums.hrms.payroll.service.StatutoryPolicyService;

@WebMvcTest(StatutoryPolicyController.class)
@Import({SecurityConfig.class, TrustedGatewayAuthenticationFilter.class})
@TestPropertySource(properties = {
        "internal.gateway.secret=test-gateway-secret",
        "spring.cloud.config.enabled=false"
})
class StatutoryPolicyControllerSecurityTests {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID POLICY_ID = UUID.randomUUID();

    @Autowired MockMvc mockMvc;
    @MockitoBean StatutoryPolicyService statutoryPolicyService;

    @Test
    void createRequiresStructureManagePermission() throws Exception {
        when(statutoryPolicyService.create(any(), any(), anyBoolean()))
                .thenReturn(null);

        mockMvc.perform(post("/api/v1/hrms/payroll/statutory-policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Authenticated-User", USER_ID.toString())
                        .header("X-User-Permissions", "PAYROLL_STRUCTURE_MANAGE")
                        .header("X-Internal-Gateway-Secret", "test-gateway-secret")
                        .content(validCreateJson()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/hrms/payroll/statutory-policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Authenticated-User", USER_ID.toString())
                        .header("X-User-Permissions", "PAYROLL_READ")
                        .header("X-Internal-Gateway-Secret", "test-gateway-secret")
                        .content(validCreateJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listAndGetRequireReadPermission() throws Exception {
        when(statutoryPolicyService.list(any(), any(), any(), anyBoolean()))
                .thenReturn(List.of());
        when(statutoryPolicyService.get(any(), any(), any(), anyBoolean()))
                .thenReturn(null);

        mockMvc.perform(get("/api/v1/hrms/payroll/statutory-policies")
                        .param("organizationId", ORGANIZATION_ID.toString())
                        .header("X-Authenticated-User", USER_ID.toString())
                        .header("X-User-Permissions", "PAYROLL_READ")
                        .header("X-Internal-Gateway-Secret", "test-gateway-secret"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/hrms/payroll/statutory-policies/{id}", POLICY_ID)
                        .param("organizationId", ORGANIZATION_ID.toString())
                        .header("X-Authenticated-User", USER_ID.toString())
                        .header("X-User-Permissions", "PAYROLL_READ")
                        .header("X-Internal-Gateway-Secret", "test-gateway-secret"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/hrms/payroll/statutory-policies")
                        .param("organizationId", ORGANIZATION_ID.toString())
                        .header("X-Authenticated-User", USER_ID.toString())
                        .header("X-User-Permissions", "PAYROLL_STRUCTURE_MANAGE")
                        .header("X-Internal-Gateway-Secret", "test-gateway-secret"))
                .andExpect(status().isForbidden());
    }

    @Test
    void spoofedHeadersWithoutGatewaySecretRemainUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/hrms/payroll/statutory-policies")
                        .param("organizationId", ORGANIZATION_ID.toString())
                        .header("X-Authenticated-User", USER_ID.toString())
                        .header("X-User-Permissions", "PAYROLL_READ"))
                .andExpect(status().isForbidden());
    }

    private String validCreateJson() {
        return """
                {
                  "organizationId":"%s",
                  "countryCode":"IN",
                  "policyVersion":"IN-2026.1",
                  "effectiveFrom":"2026-08-01",
                  "pfEmployeeRate":0.120000,
                  "pfEmployerRate":0.120000,
                  "pfContributionWageCeiling":15000.00,
                  "esiEmployeeRate":0.007500,
                  "esiEmployerRate":0.032500,
                  "esiWageEligibilityCeiling":21000.00
                }
                """.formatted(ORGANIZATION_ID);
    }
}