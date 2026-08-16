import { useEffect, useState } from "react";
import {
  Activity,
  Building2,
  Clock,
  KeyRound,
  ShieldAlert,
  UserCheck,
  UserPlus,
  Users,
} from "lucide-react";
import { ErrorState } from "../../components/ui/ErrorState";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { StatCard } from "../../components/ui/StatCard";
import { adminApi, type DashboardResponse } from "../../lib/api";

export function DashboardPage() {
  const [dashboard, setDashboard] = useState<DashboardResponse>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  useEffect(() => {
    adminApi
      .dashboard()
      .then(setDashboard)
      .catch((err: Error) =>
        setError(`Dashboard data is unavailable: ${err.message}`),
      )
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <section className="page">
        <PageHeader
          eyebrow="Command Center"
          title="IAM Dashboard"
          description="Operational overview for identity health, tenant activity, entitlement risk, and audit readiness."
        />
        <LoadingState label="Loading dashboard" />
      </section>
    );
  }

  if (error || !dashboard) {
    return (
      <section className="page">
        <PageHeader
          eyebrow="Command Center"
          title="IAM Dashboard"
          description="Operational overview for identity health, tenant activity, entitlement risk, and audit readiness."
        />
        <ErrorState
          message={error ?? "Dashboard data is unavailable."}
        />
      </section>
    );
  }

  const { users, organizations, roles, audit } = dashboard;

  const metrics = [
    {
      label: "Total users",
      value: users.total,
      icon: Users,
      helper: "All identities",
    },
    {
      label: "Active users",
      value: users.active,
      icon: UserCheck,
      helper: "Can sign in",
    },
    {
      label: "Locked users",
      value: users.locked,
      icon: ShieldAlert,
      helper: "Needs admin review",
    },
    {
      label: "Organizations",
      value: organizations.total,
      icon: Building2,
      helper: "Tenant count",
    },
    {
      label: "Pending invites",
      value: organizations.pendingInvitations,
      icon: UserPlus,
      helper: "Awaiting acceptance",
    },
    {
      label: "Total roles",
      value: roles.total,
      icon: KeyRound,
      helper: "Entitlement catalog",
    },
    {
      label: "Audit events",
      value: audit.eventsLast24Hours,
      icon: Activity,
      helper: "Last 24 hours",
    },
    {
      label: "Failed logins",
      value: audit.failedLogins,
      icon: Clock,
      helper: "Last 24 hours",
    },
  ];

  return (
    <section className="page">
      <PageHeader
        eyebrow="Command Center"
        title="IAM Dashboard"
        description="Operational overview for identity health, tenant activity, entitlement risk, and audit readiness."
      />

      <div className="metric-grid">
        {metrics.map((metric) => (
          <StatCard key={metric.label} {...metric} />
        ))}
      </div>

      <section className="panel">
        <h2>Risk Signals</h2>
        <ul className="detail-list">
          <li>
            {users.suspended} suspended users require periodic review
          </li>
          <li>
            {users.locked} locked users require account review
          </li>
          <li>
            {audit.failedLogins} failed logins recorded in the last 24 hours
          </li>
        </ul>
      </section>
    </section>
  );
}
