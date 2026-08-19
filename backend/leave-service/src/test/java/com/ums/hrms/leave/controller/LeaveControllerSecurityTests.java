package com.ums.hrms.leave.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class LeaveControllerSecurityTests {

    @Test
    void createRequiresLeaveRequestCreatePermission() throws Exception {
        Method method = LeaveController.class.getMethod(
                "create",
                com.ums.hrms.leave.dto.CreateLeaveRequest.class,
                org.springframework.security.core.Authentication.class);

        assertEquals(
                "hasAuthority('LEAVE_REQUEST_CREATE')",
                method.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void listRequiresLeaveReadPermission() throws Exception {
        Method method = LeaveController.class.getMethod(
                "list",
                java.util.UUID.class,
                int.class,
                int.class,
                org.springframework.security.core.Authentication.class);

        assertEquals(
                "hasAuthority('LEAVE_READ')",
                method.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void getRequiresLeaveReadPermission() throws Exception {
        Method method = LeaveController.class.getMethod(
                "get",
                java.util.UUID.class,
                java.util.UUID.class,
                org.springframework.security.core.Authentication.class);

        assertEquals(
                "hasAuthority('LEAVE_READ')",
                method.getAnnotation(PreAuthorize.class).value());
    }
}
