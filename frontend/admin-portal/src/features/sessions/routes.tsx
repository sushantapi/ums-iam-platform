import { Route } from "react-router-dom";
import { RequireCapability } from "../../lib/auth/RequireCapability";
import { SessionsPage } from "./SessionsPage";

export const sessionRoutes = (
  <Route path="/operations/sessions" element={<RequireCapability capability="sessions.read"><SessionsPage /></RequireCapability>} />
);
