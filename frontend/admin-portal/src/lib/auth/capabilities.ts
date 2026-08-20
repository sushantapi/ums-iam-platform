import { useAuthStore } from "../../store/authStore";

export const adminCapabilities = [
  "dashboard.read",
  "users.read",
  "users.manage",
  "organizations.read",
  "organizations.manage",
  "roles.read",
  "roles.manage",
  "sessions.read",
  "sessions.revoke",
  "audit.read",
  "hrms.employees.read",
  "hrms.employees.create",
  "hrms.employees.update",
  "hrms.departments.read",
  "hrms.departments.create",
  "hrms.departments.update",
  "hrms.designations.read",
  "hrms.designations.create",
  "hrms.designations.update",
  "hrms.attendance.read",
  "hrms.attendance.create",
  "hrms.attendance.update",
  "hrms.leave.read",
  "hrms.leave.request",
  "hrms.leave.approve",
  "hrms.leave.cancel",
  "hrms.payroll.read",
  "hrms.payroll.structure.manage",
  "hrms.payroll.run.manage",
] as const;

export type AdminCapability = (typeof adminCapabilities)[number];

const permissionByCapability: Record<AdminCapability, string> = {
  "dashboard.read": "DASHBOARD_READ",
  "users.read": "USER_READ",
  "users.manage": "USER_WRITE",
  "organizations.read": "ORGANIZATION_READ",
  "organizations.manage": "ORGANIZATION_WRITE",
  "roles.read": "ROLE_READ",
  "roles.manage": "ROLE_WRITE",
  "sessions.read": "SESSION_READ",
  "sessions.revoke": "SESSION_WRITE",
  "audit.read": "AUDIT_READ",
  "hrms.employees.read": "EMPLOYEE_READ",
  "hrms.employees.create": "EMPLOYEE_CREATE",
  "hrms.employees.update": "EMPLOYEE_UPDATE",
  "hrms.departments.read": "DEPARTMENT_READ",
  "hrms.departments.create": "DEPARTMENT_CREATE",
  "hrms.departments.update": "DEPARTMENT_UPDATE",
  "hrms.designations.read": "DESIGNATION_READ",
  "hrms.designations.create": "DESIGNATION_CREATE",
  "hrms.designations.update": "DESIGNATION_UPDATE",
  "hrms.attendance.read": "ATTENDANCE_READ",
  "hrms.attendance.create": "ATTENDANCE_CREATE",
  "hrms.attendance.update": "ATTENDANCE_UPDATE",
  "hrms.leave.read": "LEAVE_READ",
  "hrms.leave.request": "LEAVE_REQUEST_CREATE",
  "hrms.leave.approve": "LEAVE_APPROVE",
  "hrms.leave.cancel": "LEAVE_CANCEL",
  "hrms.payroll.read": "PAYROLL_READ",
  "hrms.payroll.structure.manage": "PAYROLL_STRUCTURE_MANAGE",
  "hrms.payroll.run.manage": "PAYROLL_RUN_MANAGE",
};

type JwtClaims = {
  type?: unknown;
  roles?: unknown;
  permissions?: unknown;
};

function stringArray(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }

  return value.filter(
    (item): item is string => typeof item === "string",
  );
}

function decodeClaims(token: string): JwtClaims | null {
  try {
    const parts = token.split(".");

    if (parts.length !== 3) {
      return null;
    }

    const normalized = parts[1]
      .replace(/-/g, "+")
      .replace(/_/g, "/");

    const padded = normalized.padEnd(
      Math.ceil(normalized.length / 4) * 4,
      "=",
    );

    const binary = atob(padded);
    const bytes = Uint8Array.from(
      binary,
      (character) => character.charCodeAt(0),
    );

    const decoded = new TextDecoder().decode(bytes);
    const claims = JSON.parse(decoded) as JwtClaims;

    if (claims.type !== "ACCESS") {
      return null;
    }

    return claims;
  } catch {
    return null;
  }
}

export function getAdminPermissions(): Set<string> {
  const accessToken = useAuthStore.getState().accessToken;

  if (!accessToken) {
    return new Set();
  }

  const claims = decodeClaims(accessToken);

  if (!claims) {
    return new Set();
  }

  return new Set(stringArray(claims.permissions));
}

export function getAdminRoles(): Set<string> {
  const accessToken = useAuthStore.getState().accessToken;

  if (!accessToken) {
    return new Set();
  }

  const claims = decodeClaims(accessToken);

  if (!claims) {
    return new Set();
  }

  return new Set(stringArray(claims.roles));
}

export function getAdminCapabilities(): Set<AdminCapability> {
  const permissions = getAdminPermissions();
  const capabilities = new Set<AdminCapability>();

  for (const capability of adminCapabilities) {
    const requiredPermission = permissionByCapability[capability];

    if (permissions.has(requiredPermission)) {
      capabilities.add(capability);
    }
  }

  return capabilities;
}

export function hasAdminCapability(
  capability: AdminCapability,
): boolean {
  return getAdminCapabilities().has(capability);
}
