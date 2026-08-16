import type { AuditLogResponse, PageResponse } from "../lib/api";

export const mockAuditEvents: PageResponse<AuditLogResponse> = {
  content: [
    {
      id: 1,
      auditId: "audit-001",
      eventId: "evt-001",
      eventType: "authorization.grant.created",
      action: "ASSIGN_ROLE",
      actor: "admin@example.com",
      target: "test@demo.com",
      username: "Admin User",
      userEmail: "test@demo.com",
      serviceName: "authorization-service",
      entityType: "ROLE_ASSIGNMENT",
      entityId: "assignment-001",
      ipAddress: "127.0.0.1",
      outcome: "SUCCESS",
      details: "Role ORG_ADMIN assigned",
      timestamp: "2026-06-21T09:00:00",
      createdAt: "2026-06-21T09:00:00",
    },
  ],
  page: 0,
  size: 50,
  totalElements: 1,
  totalPages: 1,
};
