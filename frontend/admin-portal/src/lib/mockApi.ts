import {
  mockAuditEvents,
  mockDashboard,
  mockGrants,
  mockOrganizations,
  mockPermissions,
  mockRoles,
  mockSessions,
  mockUserDetail,
  mockUsers,
} from "../mocks";
import type {
  OrganizationSecurityPolicyResponse,
  PageResponse,
  UserSessionResponse,
} from "./api";

function clone<T>(value: T): T {
  return structuredClone(value);
}

function pageFrom<T>(source: PageResponse<T>, url: URL): PageResponse<T> {
  return {
    ...clone(source),
    page: Number(url.searchParams.get("page") ?? source.page),
    size: Number(url.searchParams.get("size") ?? source.size),
  };
}

const defaultSecurityPolicy: OrganizationSecurityPolicyResponse = {
  organizationId: "org-acme",
  mfaRequired: true,
  sessionTimeoutMinutes: 480,
  passwordPolicyRef: "standard",
  inviteExpiryHours: 72,
  invitedByRoles: ["ORG_ADMIN"],
  roleAssignmentRoles: ["ORG_ADMIN"],
  selfServiceJoinEnabled: false,
  inviteResendLimit: 3,
  defaultInviteTemplate: "default",
  auditSeverity: "MEDIUM",
};

export async function mockRequest<T>(path: string, init?: RequestInit): Promise<T> {
  await new Promise((resolve) => window.setTimeout(resolve, 120));
  const url = new URL(path, window.location.origin);
  const method = init?.method ?? "GET";

  if (method !== "GET") {
    if (method === "PATCH" && url.pathname.endsWith("/security-policy")) {
      const changes = init?.body ? JSON.parse(String(init.body)) : {};
      return { ...defaultSecurityPolicy, ...changes } as T;
    }
    return undefined as T;
  }

  if (url.pathname === "/api/v1/admin/dashboard") return clone(mockDashboard) as T;
  if (url.pathname === "/api/v1/admin/users") return pageFrom(mockUsers, url) as T;
  if (url.pathname === "/api/v1/admin/sessions") return pageFrom(mockSessions, url) as T;
  if (url.pathname === "/api/v1/admin/organizations") return pageFrom(mockOrganizations, url) as T;
  if (url.pathname === "/api/v1/admin/roles") {
    return (url.search ? pageFrom(mockRoles, url) : clone(mockRoles.content)) as T;
  }
  if (url.pathname === "/api/v1/admin/permissions") return pageFrom(mockPermissions, url) as T;
  if (url.pathname === "/api/v1/admin/grants") return pageFrom(mockGrants, url) as T;
  if (url.pathname === "/api/v1/audit/events") return pageFrom(mockAuditEvents, url) as T;

  const userMatch = url.pathname.match(/^\/api\/v1\/admin\/users\/([^/]+)$/);
  if (userMatch) return { ...clone(mockUserDetail), id: userMatch[1] } as T;
  if (url.pathname.endsWith("/roles")) return clone(mockRoles.content) as T;
  if (url.pathname.endsWith("/organizations")) return [] as T;
  if (url.pathname.endsWith("/sessions")) {
    return clone(
      mockSessions.content.map<UserSessionResponse>((session) => ({
        id: session.id,
        device: session.device,
        ipAddress: session.ipAddress,
        lastSeenAt: session.lastSeenAt,
        createdAt: session.issuedAt,
      })),
    ) as T;
  }
  if (url.pathname.endsWith("/audit")) return pageFrom(mockAuditEvents, url) as T;

  const organizationMatch = url.pathname.match(/^\/api\/v1\/admin\/organizations\/([^/]+)$/);
  if (organizationMatch) {
    return { ...clone(mockOrganizations.content[0]), id: organizationMatch[1] } as T;
  }
  if (url.pathname.endsWith("/members") || url.pathname.endsWith("/invitations")) {
    return { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 } as T;
  }
  if (url.pathname.endsWith("/security-policy")) return clone(defaultSecurityPolicy) as T;

  const roleMatch = url.pathname.match(/^\/api\/v1\/admin\/roles\/([^/]+)$/);
  if (roleMatch) return { ...clone(mockRoles.content[0]), id: roleMatch[1] } as T;
  if (url.pathname.endsWith("/permissions")) return clone(mockPermissions.content) as T;
  if (url.pathname.endsWith("/assignments")) return pageFrom(mockGrants, url) as T;

  const eventMatch = url.pathname.match(/^\/api\/v1\/audit\/events\/([^/]+)$/);
  if (eventMatch) {
    return { ...clone(mockAuditEvents.content[0]), eventId: eventMatch[1] } as T;
  }

  throw new Error(`No mock handler for ${method} ${url.pathname}`);
}
