import {
  Navigate,
  Route,
  Routes,
} from "react-router-dom";

import { AdminShell } from "../components/layout/AdminShell";
import { RequireAuth } from "../lib/auth/RequireAuth";
import { LoginPage } from "../features/auth/LoginPage";
import { auditRoutes } from "../features/audit/routes";
import { dashboardRoutes } from "../features/dashboard/routes";
import { grantRoutes } from "../features/grants/routes";
import { BlueprintPage } from "../features/iam/BlueprintPage";
import { screenBlueprints } from "../features/iam/screenBlueprints";
import { organizationRoutes } from "../features/organizations/routes";
import { permissionRoutes } from "../features/permissions/routes";
import { roleRoutes } from "../features/roles/routes";
import { sessionRoutes } from "../features/sessions/routes";
import { userRoutes } from "../features/users/routes";

export function AppRouter() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route
        element={
          <RequireAuth>
            <AdminShell />
          </RequireAuth>
        }
      >
        <Route
          index
          element={<Navigate to="/dashboard" replace />}
        />

        {dashboardRoutes}
        {userRoutes}
        {organizationRoutes}
        {roleRoutes}
        {permissionRoutes}
        {grantRoutes}
        {auditRoutes}
        {sessionRoutes}

        {screenBlueprints
          .filter((screen) => !screen.hasCustomStarter)
          .map((screen) => (
            <Route
              key={screen.path}
              path={screen.path}
              element={<BlueprintPage screen={screen} />}
            />
          ))}
      </Route>

      <Route
        path="*"
        element={<Navigate to="/dashboard" replace />}
      />
    </Routes>
  );
}