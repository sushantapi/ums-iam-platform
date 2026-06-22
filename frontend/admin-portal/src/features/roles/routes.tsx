import { Route } from "react-router-dom";
import { RequireCapability } from "../../lib/auth/RequireCapability";
import { RoleDetailPage } from "./RoleDetailPage";
import { RoleAssignmentsPage } from "./RoleAssignmentsPage";
import { RolesPage } from "./RolesPage";

export const roleRoutes = (
  <>
    <Route path="/roles" element={<RequireCapability capability="roles.read"><RolesPage /></RequireCapability>} />
    <Route path="/roles/:roleId" element={<RequireCapability capability="roles.read"><RoleDetailPage /></RequireCapability>} />
    <Route path="/roles/assignments" element={<RequireCapability capability="roles.manage"><RoleAssignmentsPage /></RequireCapability>} />
  </>
);
