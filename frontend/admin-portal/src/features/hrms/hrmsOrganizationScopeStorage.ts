const storageKey = "ums-hrms-organization-id";

export function getStoredHrmsOrganizationId(): string {
  return window.localStorage.getItem(storageKey) ?? "";
}

export function setStoredHrmsOrganizationId(organizationId: string): void {
  if (organizationId) {
    window.localStorage.setItem(storageKey, organizationId);
    return;
  }

  window.localStorage.removeItem(storageKey);
}
