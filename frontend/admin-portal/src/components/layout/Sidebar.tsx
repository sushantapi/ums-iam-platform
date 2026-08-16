import { NavLink } from "react-router-dom";
import { sidebarSections } from "../../features/iam/screenBlueprints";
import { StatusBadge } from "../ui/StatusBadge";

export function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="brand-block">
        <div className="brand-mark">UMS</div>
        <div>
          <strong>IAM Admin</strong>
          <span>Control plane</span>
        </div>
      </div>
      <nav className="sidebar-nav" aria-label="Admin navigation">
        {sidebarSections.map(({ section, screens }) => (
          <section key={section} className="nav-section">
            <h2>{section}</h2>
            {screens.map((screen) => {
              const Icon = screen.icon;
              return (
                <NavLink key={screen.path} to={screen.path} className="nav-link">
                  <Icon size={17} />
                  <span>{screen.title}</span>
                  {screen.status === "Live starter" && <StatusBadge status="Live starter" />}
                </NavLink>
              );
            })}
          </section>
        ))}
      </nav>
    </aside>
  );
}
