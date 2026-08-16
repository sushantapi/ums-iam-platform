import type { OrganizationResponse, PageResponse } from "../lib/api";

export const mockOrganizations: PageResponse<OrganizationResponse> = {
  content: [
    {
      id: "org-acme",
      name: "Acme",
      slug: "acme",
      status: "ACTIVE",
      memberCount: 42,
      invitationsCount: 3,
      createdAt: "2026-05-20T09:00:00",
    },
  ],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
};
