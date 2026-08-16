import { Route } from "react-router-dom";
import { RequireCapability } from "../../lib/auth/RequireCapability";
import { AuditDetailPage } from "./AuditDetailPage";
import { AuditLogsPage } from "./AuditLogsPage";

export const auditRoutes = (
  <>
    <Route path="/audit" element={<RequireCapability capability="audit.read"><AuditLogsPage /></RequireCapability>} />
    <Route path="/audit/logs" element={<RequireCapability capability="audit.read"><AuditLogsPage /></RequireCapability>} />
    <Route path="/audit/:eventId" element={<RequireCapability capability="audit.read"><AuditDetailPage /></RequireCapability>} />
  </>
);
