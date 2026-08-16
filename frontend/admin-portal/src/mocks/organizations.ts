import type { OrganizationResponse, PageResponse } from "../lib/api";

export const mockOrganizations: PageResponse<OrganizationResponse> = {
  content: [
    {
      id: "11111111-1111-4111-8111-111111111111",
      name: "Acme",
      slug: "acme",
      description: "Example organization",
      ownerId: "22222222-2222-4222-8222-222222222222",
      status: "ACTIVE",
    },
  ],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
};
