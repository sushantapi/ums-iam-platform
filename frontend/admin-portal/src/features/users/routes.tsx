import { Route } from "react-router-dom";
import { RequireCapability } from "../../lib/auth/RequireCapability";
import { UserDetailPage } from "./UserDetailPage";
import { UsersPage } from "./UsersPage";

export const userRoutes = (
  <>
    <Route path="/users" element={<RequireCapability capability="users.read"><UsersPage /></RequireCapability>} />
    <Route path="/users/:userId" element={<RequireCapability capability="users.read"><UserDetailPage /></RequireCapability>} />
  </>
);
