import type { ReactNode } from "react";
import {
  Navigate,
  useLocation,
} from "react-router-dom";

import { useAuthStore } from "../../store/authStore";

export function RequireAuth({
  children,
}: {
  children: ReactNode;
}) {
  const location = useLocation();
  const accessToken = useAuthStore(
    (state) => state.accessToken,
  );

  if (!accessToken) {
    return (
      <Navigate
        to="/login"
        replace
        state={{
          from: `${location.pathname}${location.search}`,
        }}
      />
    );
  }

  return <>{children}</>;
}