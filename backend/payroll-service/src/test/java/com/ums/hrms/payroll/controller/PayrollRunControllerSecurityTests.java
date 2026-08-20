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
import com.ums.hrms.payroll.service.PayrollRunService;

@WebMvcTest(PayrollRunController.class)
@Import({SecurityConfig.class, TrustedGatewayAuthenticationFilter.class})
@TestPropertySource(properties = "internal.gateway.secret=test-gateway-secret")
class PayrollRunControllerSecurityTests {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID RUN_ID = UUID.randomUUID();
    private static final UUID ENTRY_ID = UUID.randomUUID();

    @Autowired MockMvc mockMvc;
    @MockitoBean PayrollRunService payrollRunService;

    @Test
    void createAndProcessRequireRunManagePermission() throws Exception {
        when(payrollRunService.create(any(), any(), anyBoolean())).thenReturn(null);
        when(payrollRunService.process(any(), any(), any(), anyBoolean())).thenReturn(null);

        mockMvc.perform(post("/api/v1/hrms/payroll/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Authenticated-User", USER_ID.toString())
                        .header("X-User-Permissions", "PAYROLL_RUN_MANAGE")
                        .header("X-Internal-Gateway-Secret", "test-gateway-secret")
                        .content(createJson()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/hrms/payroll/runs/" + RUN_ID + "/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Authenticated-User", USER_ID.toString())
                        .header("X-User-Permissions", "PAYROLL_RUN_MANAGE")
                        .header("X-Internal-Gateway-Secret", "test-gateway-secret")
                        .content(transitionJson()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/hrms/payroll/runs/" + RUN_ID + "/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Authenticated-User", USER_ID.toString())
                        .header("X-User-Permissions", "PAYROLL_READ")
                        .header("X-Internal-Gateway-Secret", "test-gateway-secret")
                        .content(transitionJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void runAndPayslipReadsRequireReadPermission() throws Exception {
        when(payrollRunService.list(any(), any(), anyBoolean())).thenReturn(List.of());
        when(payrollRunService.listEntries(any(), any(), any(), anyBoolean())).thenReturn(List.of());
        when(payrollRunService.getPayslip(any(), any(), any(), anyBoolean())).thenReturn(null);

        mockMvc.perform(get("/api/v1/hrms/payroll/runs")
                        .param("organizationId", ORGANIZATION_ID.toString())
                        .header("X-Authenticated-User", USER_ID.toString())
                        .header("X-User-Permissions", "PAYROLL_READ")
                        .header("X-Internal-Gateway-Secret", "test-gateway-secret"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/hrms/payroll/runs/" + RUN_ID + "/entries")
                        .param("organizationId", ORGANIZATION_ID.toString())
                        .header("X-Authenticated-User", USER_ID.toString())
                        .header("X-User-Permissions", "PAYROLL_READ")
                        .header("X-Internal-Gateway-Secret", "test-gateway-secret"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/hrms/payroll/payslips/" + ENTRY_ID)
                        .param("organizationId", ORGANIZATION_ID.toString())
                        .header("X-Authenticated-User", USER_ID.toString())
                        .header("X-User-Permissions", "PAYROLL_READ")
                        .header("X-Internal-Gateway-Secret", "test-gateway-secret"))
                .andExpect(status().isOk());
    }

    @Test
    void spoofedPayrollHeadersWithoutGatewaySecretRemainUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/hrms/payroll/runs")
                        .param("organizationId", ORGANIZATION_ID.toString())
                        .header("X-Authenticated-User", USER_ID.toString())
                        .header("X-User-Permissions", "PAYROLL_READ"))
                .andExpect(status().isForbidden());
    }

    private String createJson() {
        return """
                {
                  "organizationId":"%s",
                  "payrollMonth":"2026-08"
                }
                """.formatted(ORGANIZATION_ID);
    }

    private String transitionJson() {
        return """
                {
                  "organizationId":"%s"
                }
                """.formatted(ORGANIZATION_ID);
    }
}
