import type { GrantResponse, PageResponse } from "../lib/api";

export const mockGrants: PageResponse<GrantResponse> = {
  content: [
    {
      id: "grant-001",
      principal: "Sushant Kumar",
      principalId: "7b3c58f8-fd3b-4d3c-8b21-18836d985e21",
      principalType: "USER",
      roleId: "role-org-admin",
      roleName: "ORG_ADMIN",
      organizationId: "org-acme",
      organizationName: "Acme",
      scope: "organization",
      assignedBy: "admin@example.com",
      assignedAt: "2026-06-21T09:00:00",
      status: "ACTIVE",
      source: "direct",
    },
  ],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
};
