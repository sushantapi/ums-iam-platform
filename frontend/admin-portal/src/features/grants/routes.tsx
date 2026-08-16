import { Route } from "react-router-dom";
import { RequireCapability } from "../../lib/auth/RequireCapability";
import { GrantsPage } from "./GrantsPage";

export const grantRoutes = (
  <Route path="/grants" element={<RequireCapability capability="roles.read"><GrantsPage /></RequireCapability>} />
);
