import { Menu, Search } from "lucide-react";
import { Outlet } from "react-router-dom";
import { Sidebar } from "./Sidebar";

export function AdminShell() {
  return (
    <div className="admin-shell">
      <Sidebar />
      <main className="main-panel">
        <header className="topbar">
          <button className="icon-button" aria-label="Toggle navigation">
            <Menu size={18} />
          </button>
          <div className="search-box">
            <Search size={16} />
            <input placeholder="Search users, roles, tenants, events" />
          </div>
          <div className="operator-chip">Admin</div>
        </header>
        <Outlet />
      </main>
    </div>
  );
}
