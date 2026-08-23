import {
  Navigate,
  Route,
  Routes,
} from "react-router-dom";

import { AdminShell } from "../components/layout/AdminShell";
import { ForbiddenPage } from "../features/auth/ForbiddenPage";
import { ForgotPasswordPage } from "../features/auth/ForgotPasswordPage";
import { InvitationAcceptancePage } from "../features/auth/InvitationAcceptancePage";
import { LoginPage } from "../features/auth/LoginPage";
import { MfaChallengePage } from "../features/auth/MfaChallengePage";
import { MfaSecurityPage } from "../features/auth/MfaSecurityPage";
import { RegisterPage } from "../features/auth/RegisterPage";
import { ResetPasswordPage } from "../features/auth/ResetPasswordPage";
import { auditRoutes } from "../features/audit/routes";
import { dashboardRoutes } from "../features/dashboard/routes";
import { grantRoutes } from "../features/grants/routes";
import { hrmsRoutes } from "../features/hrms/routes";
import { organizationRoutes } from "../features/organizations/routes";
import { permissionRoutes } from "../features/permissions/routes";
import { roleRoutes } from "../features/roles/routes";
import { sessionRoutes } from "../features/sessions/routes";
import { userRoutes } from "../features/users/routes";
import { RequireAuth } from "../lib/auth/RequireAuth";

export function AppRouter() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/mfa-challenge" element={<MfaChallengePage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route path="/accept-invitation" element={<InvitationAcceptancePage />} />

      <Route
        element={
          <RequireAuth>
            <AdminShell />
          </RequireAuth>
        }
      >
        <Route
          path="/forbidden"
          element={<ForbiddenPage />}
        />

        <Route path="/security" element={<MfaSecurityPage />} />

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
        {hrmsRoutes}
      </Route>

      <Route
        path="*"
        element={<Navigate to="/dashboard" replace />}
      />
    </Routes>
  );
}
