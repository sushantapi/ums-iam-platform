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
] as const;

export type AdminCapability = (typeof adminCapabilities)[number];

export function getAdminCapabilities(): Set<AdminCapability> {
  const stored = localStorage.getItem("ums_admin_capabilities");
  if (!stored) return new Set(adminCapabilities);

  try {
    const parsed = JSON.parse(stored) as string[];
    return new Set(parsed.filter((value): value is AdminCapability =>
      adminCapabilities.includes(value as AdminCapability),
    ));
  } catch {
    return new Set();
  }
}

export function hasAdminCapability(capability: AdminCapability): boolean {
  return getAdminCapabilities().has(capability);
}
