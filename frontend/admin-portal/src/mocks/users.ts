import type { PageResponse, UserDetailResponse, UserSummaryResponse } from "../lib/api";

export const mockUsers: PageResponse<UserSummaryResponse> = {
  content: [
    {
      id: "7b3c58f8-fd3b-4d3c-8b21-18836d985e21",
      fullName: "Sushant Kumar",
      email: "test@demo.com",
      status: "ACTIVE",
      organizationName: "Acme",
      roles: ["ORG_ADMIN"],
      lastLoginAt: "2026-06-21T09:00:00",
    },
  ],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
};

export const mockUserDetail: UserDetailResponse = {
  id: "7b3c58f8-fd3b-4d3c-8b21-18836d985e21",
  fullName: "Sushant Kumar",
  email: "test@demo.com",
  status: "ACTIVE",
  verified: true,
  mfaEnabled: true,
  createdAt: "2026-05-20T09:00:00",
  lastLoginAt: "2026-06-21T09:00:00",
};
