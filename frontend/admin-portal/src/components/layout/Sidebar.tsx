import { NavLink } from "react-router-dom";
import { sidebarSections } from "../../features/iam/screenBlueprints";
import { getAdminCapabilities } from "../../lib/auth/capabilities";
import { useAuthStore } from "../../store/authStore";
import { StatusBadge } from "../ui/StatusBadge";

export function Sidebar() {
  const accessToken = useAuthStore((state) => state.accessToken);

  if (!accessToken) {
    return null;
  }

  const capabilities = getAdminCapabilities();

  const visibleSections = sidebarSections
    .map(({ section, screens }) => ({
      section,
      screens: screens.filter(
        (screen) =>
          screen.hasCustomStarter === true &&
          screen.requiredCapability !== undefined &&
          capabilities.has(screen.requiredCapability),
      ),
    }))
    .filter(({ screens }) => screens.length > 0);

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
        {visibleSections.map(({ section, screens }) => (
          <section key={section} className="nav-section">
            <h2>{section}</h2>

            {screens.map((screen) => {
              const Icon = screen.icon;

              return (
                <NavLink
                  key={screen.path}
                  to={screen.path}
                  className="nav-link"
                >
                  <Icon size={17} />
                  <span>{screen.title}</span>

                  {screen.status === "Live starter" && (
                    <StatusBadge status="Live starter" />
                  )}
                </NavLink>
              );
            })}
          </section>
        ))}
      </nav>
    </aside>
  );
}
