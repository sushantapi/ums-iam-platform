import type { PageResponse, PermissionResponse, RoleResponse } from "../lib/api";

export const mockRoles: PageResponse<RoleResponse> = {
  content: [
    {
      id: "role-org-admin",
      name: "ORG_ADMIN",
      scopeType: "organization",
      description: "Organization administrator",
      permissionCount: 18,
      assignedUserCount: 9,
      systemRole: true,
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
      code: "users:read",
      resource: "users",
      action: "read",
      description: "Read user records",
      systemPermission: true,
      rolesUsingPermission: 3,
    },
  ],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
};
