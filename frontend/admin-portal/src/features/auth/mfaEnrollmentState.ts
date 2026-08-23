export interface PendingMfaEnrollment {
  organizationId: string;
  returnPath: string;
}

const STORAGE_KEY = "ums-admin-mfa-enrollment";

function safePath(value: unknown): string {
  return typeof value === "string" &&
    value.startsWith("/") &&
    !value.startsWith("//")
    ? value
    : "/dashboard";
}

export function beginMfaEnrollment(
  organizationId: string,
  returnPath: string,
) {
  const pending: PendingMfaEnrollment = {
    organizationId,
    returnPath: safePath(returnPath),
  };

  window.sessionStorage.setItem(
    STORAGE_KEY,
    JSON.stringify(pending),
  );
}

export function getPendingMfaEnrollment(): PendingMfaEnrollment | null {
  try {
    const raw = window.sessionStorage.getItem(STORAGE_KEY);

    if (!raw) {
      return null;
    }

    const parsed = JSON.parse(raw) as Partial<PendingMfaEnrollment>;

    if (
      typeof parsed.organizationId !== "string" ||
      !parsed.organizationId.trim()
    ) {
      clearPendingMfaEnrollment();
      return null;
    }

    return {
      organizationId: parsed.organizationId,
      returnPath: safePath(parsed.returnPath),
    };
  } catch {
    clearPendingMfaEnrollment();
    return null;
  }
}

export function clearPendingMfaEnrollment() {
  window.sessionStorage.removeItem(STORAGE_KEY);
}
