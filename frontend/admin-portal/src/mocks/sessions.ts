import type { AdminSessionResponse, PageResponse } from "../lib/api";

export const mockSessions: PageResponse<AdminSessionResponse> = {
  content: [
    {
      id: "session-001",
      userId: "7b3c58f8-fd3b-4d3c-8b21-18836d985e21",
      userName: "Sushant Kumar",
      organizationId: "org-acme",
      organizationName: "Acme",
      device: "Chrome on Windows",
      client: "admin-portal",
      issuedAt: "2026-06-21T08:00:00",
      lastSeenAt: "2026-06-21T09:00:00",
      expiresAt: "2026-06-21T18:00:00",
      status: "ACTIVE",
    },
  ],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
};
