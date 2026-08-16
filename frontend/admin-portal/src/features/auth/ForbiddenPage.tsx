import { Link, useLocation } from "react-router-dom";

export function ForbiddenPage() {
  const location = useLocation();

  const requiredCapability = (
    location.state as {
      requiredCapability?: string;
    } | null
  )?.requiredCapability;

  return (
    <section className="page">
      <div className="page-header">
        <div>
          <span className="eyebrow">Authorization</span>
          <h1>Access denied</h1>
          <p>
            Your administrator account does not have permission
            to access this area.
          </p>
        </div>
      </div>

      <div className="empty-state">
        <strong>403 — Forbidden</strong>

        {requiredCapability ? (
          <p>
            Required capability:{" "}
            <code>{requiredCapability}</code>
          </p>
        ) : null}

        <Link
          className="button-secondary"
          to="/dashboard"
        >
          Return to dashboard
        </Link>
      </div>
    </section>
  );
}