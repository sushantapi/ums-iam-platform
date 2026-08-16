import { useEffect, useState } from "react";
import { Activity, Building2, Clock, KeyRound, ShieldAlert, UserCheck, UserPlus, Users } from "lucide-react";
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
      .catch((err: Error) => setError(`Dashboard data is unavailable: ${err.message}`))
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
        <ErrorState message={error ?? "Dashboard data is unavailable."} />
      </section>
    );
  }

  const users = dashboard.users ?? {
    total: dashboard.totalUsers,
    active: dashboard.activeUsers,
    locked: dashboard.blockedUsers,
    suspended: 0,
  };
  const organizations = dashboard.organizations ?? { total: 0, active: 0, pendingInvitations: 0 };
  const roles = dashboard.roles ?? { total: 0 };
  const audit = dashboard.audit ?? {
    eventsLast24Hours: dashboard.todayLogins,
    failedLogins: 0,
  };

  const metrics = [
    { label: "Total users", value: users.total ?? 0, icon: Users, helper: "All identities" },
    { label: "Active users", value: users.active ?? 0, icon: UserCheck, helper: "Can sign in" },
    { label: "Locked users", value: users.locked ?? 0, icon: ShieldAlert, helper: "Needs admin review" },
    { label: "Organizations", value: organizations.total ?? 0, icon: Building2, helper: "Tenant count" },
    { label: "Pending invites", value: organizations.pendingInvitations ?? 0, icon: UserPlus, helper: "Awaiting acceptance" },
    { label: "Total roles", value: roles.total ?? 0, icon: KeyRound, helper: "Entitlement catalog" },
    { label: "Audit events", value: audit.eventsLast24Hours ?? 0, icon: Activity, helper: "Last 24 hours" },
    { label: "Failed logins", value: audit.failedLogins ?? 0, icon: Clock, helper: "If available" },
  ];

  return (
    <section className="page">
      <PageHeader
        eyebrow="Command Center"
        title="IAM Dashboard"
        description="Operational overview for identity health, tenant activity, entitlement risk, and audit readiness."
        actions={<button className="button-secondary">Export snapshot</button>}
      />
      <div className="metric-grid">
        {metrics.map((metric) => (
          <StatCard key={metric.label} {...metric} />
        ))}
      </div>
      <div className="blueprint-grid">
        <section className="panel">
          <h2>Risk Signals</h2>
          <ul className="detail-list">
            <li>{users.suspended ?? 0} suspended users require periodic review</li>
            <li>{users.locked ?? 0} locked users may indicate account takeover attempts</li>
            <li>{audit.failedLogins ?? 0} failed logins should be correlated with source IPs</li>
          </ul>
        </section>
        <section className="panel">
          <h2>Operational Follow-Up</h2>
          <ul className="detail-list">
            <li>Normalize the dashboard API to nested users, organizations, roles, and audit objects</li>
            <li>Add failed login aggregation from authentication or audit events</li>
            <li>Back the invitation count from organization-service invitation state</li>
          </ul>
        </section>
      </div>
    </section>
  );
}
