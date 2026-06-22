import type { AuditLogResponse, PageResponse } from "../lib/api";

export const mockAuditEvents: PageResponse<AuditLogResponse> = {
  content: [
    {
      eventId: "evt-001",
      eventType: "authorization.grant.created",
      actor: "admin@example.com",
      target: "test@demo.com",
      organizationId: "org-acme",
      organization: "Acme",
      outcome: "SUCCESS",
      ipAddress: "127.0.0.1",
      serviceName: "authorization-service",
      createdAt: "2026-06-21T09:00:00",
      details: "Role ORG_ADMIN assigned",
    },
  ],
  page: 0,
  size: 50,
  totalElements: 1,
  totalPages: 1,
};
