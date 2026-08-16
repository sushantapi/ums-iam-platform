package com.ums.admin.service.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.admin.client.AuthenticationServiceClient;
import com.ums.admin.client.RoleServiceClient;

@ExtendWith(MockitoExtension.class)
class AdminRoleServiceImplTests {

    @Mock
    private RoleServiceClient roleServiceClient;

    @Mock
    private AuthenticationServiceClient authenticationServiceClient;

    @InjectMocks
    private AdminRoleServiceImpl adminRoleService;

    @Test
    void revokeRoleAssignmentRevokesAffectedUserSessions() {
        UUID assignmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        when(roleServiceClient.revokeRoleAssignment(assignmentId, actorUserId))
                .thenReturn(userId);

        adminRoleService.revokeRoleAssignment(assignmentId, actorUserId);

        InOrder inOrder = inOrder(
                roleServiceClient,
                authenticationServiceClient);

        inOrder.verify(roleServiceClient)
                .revokeRoleAssignment(assignmentId, actorUserId);

        inOrder.verify(authenticationServiceClient)
                .revokeAllSessions(userId, actorUserId);
    }

    @Test
    void revokeRoleAssignmentDoesNotRevokeSessionsWhenRoleRevokeFails() {
        UUID assignmentId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        when(roleServiceClient.revokeRoleAssignment(assignmentId, actorUserId))
                .thenThrow(new IllegalStateException("authorization unavailable"));

        assertThatThrownBy(() ->
                adminRoleService.revokeRoleAssignment(
                        assignmentId,
                        actorUserId))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(authenticationServiceClient);
    }

    @Test
    void revokeRoleAssignmentFallsBackWhenAuthenticationUnavailable() {
        UUID assignmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        when(roleServiceClient.revokeRoleAssignment(assignmentId, actorUserId))
                .thenReturn(userId);

        doThrow(new IllegalStateException("authentication unavailable"))
                .when(authenticationServiceClient)
                .revokeAllSessions(userId, actorUserId);

        assertThatCode(() ->
                adminRoleService.revokeRoleAssignment(
                        assignmentId,
                        actorUserId))
                .doesNotThrowAnyException();

        InOrder inOrder = inOrder(
                roleServiceClient,
                authenticationServiceClient);

        inOrder.verify(roleServiceClient)
                .revokeRoleAssignment(assignmentId, actorUserId);

        inOrder.verify(authenticationServiceClient)
                .revokeAllSessions(userId, actorUserId);
    }}