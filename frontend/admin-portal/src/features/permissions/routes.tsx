import { Route } from "react-router-dom";
import { RequireCapability } from "../../lib/auth/RequireCapability";
import { PermissionsPage } from "./PermissionsPage";

export const permissionRoutes = (
  <Route path="/permissions" element={<RequireCapability capability="roles.read"><PermissionsPage /></RequireCapability>} />
);
