import { Route } from "react-router-dom";
import { RequireCapability } from "../../lib/auth/RequireCapability";
import { AuditLogsPage } from "./AuditLogsPage";

export const auditRoutes = (
  <>
    <Route
      path="/audit"
      element={
        <RequireCapability capability="audit.read">
          <AuditLogsPage />
        </RequireCapability>
      }
    />
    <Route
      path="/audit/logs"
      element={
        <RequireCapability capability="audit.read">
          <AuditLogsPage />
        </RequireCapability>
      }
    />
  </>
);
