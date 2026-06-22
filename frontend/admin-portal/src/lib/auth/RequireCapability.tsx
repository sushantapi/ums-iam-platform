import type { ReactNode } from "react";
import { getAdminCapabilities, type AdminCapability } from "./capabilities";

export function RequireCapability({
  capability,
  children,
}: {
  capability: AdminCapability;
  children: ReactNode;
}) {
  if (!getAdminCapabilities().has(capability)) {
    return (
      <section className="page">
        <div className="empty-state">
          <strong>Access denied</strong>
          <span>Your administrator role does not include the required capability: {capability}.</span>
        </div>
      </section>
    );
  }

  return <>{children}</>;
}
