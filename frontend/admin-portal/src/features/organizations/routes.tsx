import { Route } from "react-router-dom";
import { RequireCapability } from "../../lib/auth/RequireCapability";
import { OrganizationDetailPage } from "./OrganizationDetailPage";
import { OrganizationMembersPage } from "./OrganizationMembersPage";
import { OrganizationsPage } from "./OrganizationsPage";

export const organizationRoutes = (
  <>
    <Route
      path="/organizations"
      element={
        <RequireCapability capability="organizations.read">
          <OrganizationsPage />
        </RequireCapability>
      }
    />
    <Route
      path="/organizations/:organizationId"
      element={
        <RequireCapability capability="organizations.read">
          <OrganizationDetailPage />
        </RequireCapability>
      }
    />
    <Route
      path="/organizations/:organizationId/members"
      element={
        <RequireCapability capability="organizations.read">
          <OrganizationMembersPage />
        </RequireCapability>
      }
    />
  </>
);
