import type { PageResponse, PermissionResponse, RoleResponse } from "../lib/api";

export const mockRoles: PageResponse<RoleResponse> = {
  content: [
    {
      id: "d2e53624-8df3-4acb-b78d-c6eab765fc88",
      name: "ORG_ADMIN",
      description: "Organization administrator",
      system: true,
      active: true,
    },
  ],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
};
export const mockPermissions: PageResponse<PermissionResponse> = {
  content: [
    {
      id: "perm-users-read",
      code: "USER_READ",
      action: "READ",
      description: "Read user records",
      active: true,
    },
  ],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
};
