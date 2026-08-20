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
import com.ums.hrms.payroll.service.SalaryStructureService;

@WebMvcTest(SalaryStructureController.class)
@Import({SecurityConfig.class, TrustedGatewayAuthenticationFilter.class})
@TestPropertySource(properties = "internal.gateway.secret=test-gateway-secret")
class SalaryStructureControllerSecurityTests {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();

    @Autowired MockMvc mockMvc;
    @MockitoBean SalaryStructureService salaryStructureService;

    @Test
    void createRequiresStructureManagePermission() throws Exception {
        when(salaryStructureService.create(any(), any(), anyBoolean())).thenReturn(null);

        mockMvc.perform(post("/api/v1/hrms/payroll/salary-structures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Authenticated-User", USER_ID.toString())
                        .header("X-User-Permissions", "PAYROLL_STRUCTURE_MANAGE")
                        .header("X-Internal-Gateway-Secret", "test-gateway-secret")
                        .content(validCreateJson()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/hrms/payroll/salary-structures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Authenticated-User", USER_ID.toString())
                        .header("X-User-Permissions", "PAYROLL_READ")
                        .header("X-Internal-Gateway-Secret", "test-gateway-secret")
                        .content(validCreateJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listRequiresReadPermission() throws Exception {
        when(salaryStructureService.list(any(), any(), any(), anyBoolean())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/hrms/payroll/salary-structures")
                        .param("organizationId", ORGANIZATION_ID.toString())
                        .param("employeeId", EMPLOYEE_ID.toString())
                        .header("X-Authenticated-User", USER_ID.toString())
                        .header("X-User-Permissions", "PAYROLL_READ")
                        .header("X-Internal-Gateway-Secret", "test-gateway-secret"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/hrms/payroll/salary-structures")
                        .param("organizationId", ORGANIZATION_ID.toString())
                        .param("employeeId", EMPLOYEE_ID.toString())
                        .header("X-Authenticated-User", USER_ID.toString())
                        .header("X-User-Permissions", "PAYROLL_STRUCTURE_MANAGE")
                        .header("X-Internal-Gateway-Secret", "test-gateway-secret"))
                .andExpect(status().isForbidden());
    }

    @Test
    void spoofedHeadersWithoutGatewaySecretRemainUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/hrms/payroll/salary-structures")
                        .param("organizationId", ORGANIZATION_ID.toString())
                        .param("employeeId", EMPLOYEE_ID.toString())
                        .header("X-Authenticated-User", USER_ID.toString())
                        .header("X-User-Permissions", "PAYROLL_READ"))
                .andExpect(status().isForbidden());
    }

    private String validCreateJson() {
        return """
                {
                  "organizationId":"%s",
                  "employeeId":"%s",
                  "basicPay":50000.00,
                  "allowanceTotal":5000.00,
                  "deductionTotal":2500.00,
                  "effectiveFrom":"2026-08-01"
                }
                """.formatted(ORGANIZATION_ID, EMPLOYEE_ID);
    }
}
