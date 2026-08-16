import { LogOut } from "lucide-react";
import { useState } from "react";
import { Outlet, useNavigate } from "react-router-dom";

import authService from "../../api/services/authService";
import { useAuthStore } from "../../store/authStore";
import { Sidebar } from "./Sidebar";

export function AdminShell() {
  const navigate = useNavigate();

  const user = useAuthStore((state) => state.user);
  const clearSession = useAuthStore((state) => state.clearSession);

  const [signingOut, setSigningOut] = useState(false);

  async function handleLogout() {
    setSigningOut(true);

    try {
      await authService.logout();
    } catch {
      // Local session is still cleared if the server is unavailable.
    } finally {
      clearSession();
      navigate("/login", { replace: true });
    }
  }

  return (
    <div className="admin-shell">
      <Sidebar />

      <main className="main-panel">
        <header className="topbar">

          <div className="operator-chip">
            {user?.email ?? "Admin"}
          </div>

          <button
            type="button"
            className="button-secondary topbar-logout"
            onClick={handleLogout}
            disabled={signingOut}
          >
            <LogOut size={16} />
            {signingOut ? "Signing out..." : "Sign out"}
          </button>
        </header>

        <Outlet />
      </main>
    </div>
  );
}