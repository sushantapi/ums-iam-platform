import type { GrantResponse, PageResponse } from "../lib/api";

export const mockGrants: PageResponse<GrantResponse> = {
  content: [
    {
      assignmentId: "7b3c58f8-fd3b-4d3c-8b21-18836d985e21",
      roleId: "dceb9e85-d696-41c8-8fc0-f0c65365fcb7",
      roleName: "ORG_ADMIN",
      scopeType: "ORG",
      scopeId: "org-acme",
      active: true,
      assignedAt: "2026-06-21T09:00:00",
      expiresAt: null,
    },
  ],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
};
