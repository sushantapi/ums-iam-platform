import { mockRequest } from "./mockApi";
import { runtimeConfig, shouldUseMock, type MockFeature } from "./runtimeConfig";
import {
  redirectToForbidden,
  redirectToLogin,
  refreshAccessToken,
} from "./auth/sessionManager";
import { useAuthStore } from "../store/authStore";

export type PageResponse<T> = {
  content: T[];
  page: number;
  totalElements: number;
  totalPages: number;
  size: number;
};

export type QueryValue = string | number | boolean | undefined;

function withQuery(path: string, query: Record<string, QueryValue>) {
  const params = new URLSearchParams();

  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== "") {
      params.set(key, String(value));
    }
  });

  const queryString = params.toString();
  return queryString ? `${path}?${queryString}` : path;
}

async function request<T>(
  feature: MockFeature,
  path: string,
  init?: RequestInit,
): Promise<T> {
  if (shouldUseMock(feature)) {
    return mockRequest<T>(path, init);
  }

  async function execute(accessToken: string | null) {
    return fetch(`${runtimeConfig.apiBaseUrl}${path}`, {
      ...init,
      headers: {
        "Content-Type": "application/json",
        ...(accessToken
          ? { Authorization: `Bearer ${accessToken}` }
          : {}),
        ...init?.headers,
      },
    });
  }

  let response = await execute(
    useAuthStore.getState().accessToken,
  );

  if (response.status === 401) {
    const refreshedAccessToken = await refreshAccessToken();

    if (refreshedAccessToken) {
      response = await execute(refreshedAccessToken);
    }
  }

  if (response.status === 401) {
    redirectToLogin();
  }

  if (response.status === 403) {
    redirectToForbidden();
  }

  if (!response.ok) {
    throw new Error(
      `Request failed: ${response.status} ${response.statusText}`,
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
export type DashboardResponse = {
  users?: {
    total?: number;
    active?: number;
    locked?: number;
    suspended?: number;
  };
  organizations?: {
    total?: number;
    active?: number;
    pendingInvitations?: number;
  };
  roles?: {
    total?: number;
  };
  audit?: {
    eventsLast24Hours?: number;
    failedLogins?: number;
  };
  totalUsers?: number;
  activeUsers?: number;
  blockedUsers?: number;
  activeSessions?: number;
  todayLogins?: number;
};

export type UserSummaryResponse = {
  id?: number | string;
  userId?: number | string;
  firstName?: string;
  lastName?: string;
  username?: string;
  email?: string;
  fullName?: string;
  status?: string;
  organizationName?: string;
  organizationId?: string;
  roles?: string[];
  lastLoginAt?: string;
  createdAt?: string;
};

export type UserDetailResponse = {
  id: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  mobile?: string;
  active?: boolean;
  status?: string;
  locked?: boolean;
  lockedUntil?: string;
  lastLoginAt?: string;
};

export type UserRoleAssignmentResponse = {
  assignmentId: string;
  roleId: string;
  roleName: string;
  scopeType?: string;
  scopeId?: string;
  active: boolean;
  assignedAt?: string;
  expiresAt?: string;
};

export type UserOrganizationResponse = {
  id: string;
  name: string;
  slug?: string;
  description?: string;
  ownerId?: string;
  status?: string;
};

export type UserSessionResponse = {
  id: string;
  userId: string;
  userName: string;
  organizationId: string | null;
  organizationName: string | null;
  device: string | null;
  client: string | null;
  ipAddress: string | null;
  issuedAt: string;
  lastSeenAt: string | null;
  expiresAt: string;
  revokedAt: string | null;
  status: string;
};

export type OrganizationResponse = {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  ownerId: string | null;
  status: string;
};

export type OrganizationMemberResponse = {
  id: string;
  userId: string;
  role: string;
  joinedAt: string;
};

export type RoleResponse = {
  id?: string;
  roleId?: string;
  name?: string;
  roleName?: string;
  scopeType?: "platform" | "organization" | string;
  description?: string;
  permissionCount?: number;
  assignedUserCount?: number;
  system?: boolean;
  active?: boolean;
  systemRole?: boolean;
  assignableBy?: string[];
};

export type PermissionResponse = {
  id: string;
  code: string;
  action: string;
  description: string | null;
  active: boolean;
};

export type AuditLogResponse = {
  id?: number | string;
  auditId?: string;
  eventId?: string;
  action?: string;
  actor?: string;
  target?: string;
  targetUser?: string;
  organizationId?: string;
  organization?: string;
  username?: string;
  userEmail?: string;
  serviceName?: string;
  module?: string;
  endpoint?: string;
  method?: string;
  ipAddress?: string;
  userAgent?: string;
  correlationId?: string;
  traceId?: string;
  eventType?: string;
  timestamp?: string;
  createdAt?: string;
  status?: string;
  outcome?: string;
  details?: string;
  metadata?: Record<string, unknown>;
  changedFields?: Array<{ field: string; before?: string; after?: string }>;
};

export type AdminSessionResponse = {
  id: string;
  userId: string;
  userName: string;
  organizationId: string | null;
  organizationName: string | null;
  device: string | null;
  client: string | null;
  ipAddress: string | null;
  issuedAt: string;
  lastSeenAt: string | null;
  expiresAt: string;
  revokedAt: string | null;
  status: string;
};

export type GrantResponse = {
  assignmentId: string;
  roleId: string;
  roleName: string;
  scopeType: string;
  scopeId: string;
  active: boolean;
  assignedAt: string;
  expiresAt: string | null;
};

function normalizePage<T>(payload: T[] | PageResponse<T>, page: number, size: number): PageResponse<T> {
  if (Array.isArray(payload)) {
    return {
      content: payload,
      totalElements: payload.length,
      totalPages: payload.length > 0 ? 1 : 0,
      page,
      size,
    };
  }

  return payload;
}

export const adminApi = {
  dashboard: () => request<DashboardResponse>("dashboard", "/api/v1/admin/dashboard"),
  users: async (query: {
    page: number;
    size: number;
    search?: string;
  }) => {
    const payload = await request<UserSummaryResponse[] | PageResponse<UserSummaryResponse>>(
      "users",
      withQuery("/api/v1/admin/users", query),
    );
    return normalizePage(payload, query.page, query.size);
  },
  userDetail: (userId: string) =>
    request<UserDetailResponse>("users", `/api/v1/admin/users/${userId}`),
  userRoles: (userId: string) =>
    request<UserRoleAssignmentResponse[]>("roles", `/api/v1/admin/users/${userId}/roles`),
  userOrganizations: (userId: string) =>
    request<UserOrganizationResponse[]>(
      "organizations",
      `/api/v1/admin/users/${userId}/organizations`,
    ),
  userSessions: (userId: string) =>
    request<UserSessionResponse[]>("sessions", `/api/v1/admin/users/${userId}/sessions`),
  revokeUserSession: (userId: string, sessionId: string) =>
    request<void>("sessions", `/api/v1/admin/sessions/${sessionId}/revoke`, {
      method: "POST",
    }),
  revokeAllUserSessions: (userId: string) =>
    request<void>("sessions", `/api/v1/admin/users/${userId}/sessions/revoke-all`, {
      method: "POST",
    }),
  userAudit: async (userId: string, query: { page: number; size: number }) => {
    const payload = await request<AuditLogResponse[] | PageResponse<AuditLogResponse>>(
      "audit",
      withQuery("/api/v1/audit/events", { ...query, target: userId }),
    );
    return normalizePage(payload, query.page, query.size);
  },
  sessions: (query: {
    page: number;
    size: number;
    userId?: string;
    organizationId?: string;
    status?: string;
    from?: string;
    to?: string;
  }) =>
    request<PageResponse<AdminSessionResponse>>(
      "sessions",
      withQuery("/api/v1/admin/sessions", query),
    ),
  revokeSession: (sessionId: string) =>
    request<void>("sessions", `/api/v1/admin/sessions/${sessionId}/revoke`, { method: "POST" }),
  activateUser: (userId: string) =>
    request<void>("users", `/api/v1/admin/users/${userId}/activate`, { method: "POST" }),
  suspendUser: (userId: string) =>
    request<void>("users", `/api/v1/admin/users/${userId}/suspend`, { method: "POST" }),
  unlockUser: (userId: string) =>
    request<void>("users", `/api/v1/admin/users/${userId}/unlock`, { method: "POST" }),
  roles: () =>
    request<RoleResponse[]>("roles", "/api/v1/admin/roles"),
  roleDetail: (roleId: string) =>
    request<RoleResponse>("roles", `/api/v1/admin/roles/${roleId}`),
  rolePermissions: (roleId: string) =>
    request<PermissionResponse[]>("roles", `/api/v1/admin/roles/${roleId}/permissions`),
  permissions: () =>
    request<PermissionResponse[]>(
      "permissions",
      "/api/v1/admin/permissions",
    ),
  auditLogs: async (query: {
    page: number;
    size: number;
    actor?: string;
    target?: string;
    organizationId?: string;
    eventType?: string;
    serviceName?: string;
    outcome?: string;
    from?: string;
    to?: string;
  }) => {
    const payload = await request<AuditLogResponse[] | PageResponse<AuditLogResponse>>(
      "audit",
      withQuery("/api/v1/audit/events", query),
    );
    return normalizePage(payload, query.page, query.size);
  },
  assignRole: (body: {
    userId: string;
    roleName: string;
    scopeType: "PLATFORM" | "ORG" | "DEPARTMENT";
    scopeId: string;
  }) =>
    request<string>("roles", "/api/v1/admin/roles/assign", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  revokeRole: (userId: string, roleId: string) =>
    request<void>("roles", `/api/v1/authorization/users/${userId}/roles/${roleId}`, {
      method: "DELETE",
    }),
  grants: (query: { page: number; size: number }) =>
    request<PageResponse<GrantResponse>>(
      "grants",
      withQuery("/api/v1/admin/grants", query),
    ),
  revokeGrant: (grantId: string) =>
    request<void>("grants", `/api/v1/admin/grants/${grantId}`, { method: "DELETE" }),
  organizations: async (query: { page: number; size: number; search?: string }) => {
    const payload = await request<OrganizationResponse[] | PageResponse<OrganizationResponse>>(
      "organizations",
      withQuery("/api/v1/admin/organizations", query),
    );
    return normalizePage(payload, query.page, query.size);
  },
  organizationDetail: (organizationId: string) =>
    request<OrganizationResponse>(
      "organizations",
      `/api/v1/admin/organizations/${organizationId}`,
    ),
  organizationMembers: (organizationId: string) =>
    request<OrganizationMemberResponse[]>(
      "organizations",
      `/api/v1/admin/organizations/${organizationId}/members`,
    ),
  auditEvent: (eventId: string) =>
    request<AuditLogResponse>("audit", `/api/v1/audit/events/${eventId}`),
};
