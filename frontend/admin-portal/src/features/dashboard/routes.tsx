import { Route } from "react-router-dom";

import { RequireCapability } from "../../lib/auth/RequireCapability";
import { DashboardPage } from "./DashboardPage";

export const dashboardRoutes = (
  <Route
    path="/dashboard"
    element={
      <RequireCapability capability="dashboard.read">
        <DashboardPage />
      </RequireCapability>
    }
  />
);