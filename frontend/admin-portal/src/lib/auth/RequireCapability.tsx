import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";

import {
  hasAdminCapability,
  type AdminCapability,
} from "./capabilities";

export function RequireCapability({
  capability,
  children,
}: {
  capability: AdminCapability;
  children: ReactNode;
}) {
  if (!hasAdminCapability(capability)) {
    return (
      <Navigate
        to="/forbidden"
        replace
        state={{ requiredCapability: capability }}
      />
    );
  }

  return <>{children}</>;
}