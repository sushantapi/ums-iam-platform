import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { EntitySummaryCard } from "../../components/ui/EntitySummaryCard";
import { ErrorState } from "../../components/ui/ErrorState";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { adminApi, type OrganizationResponse } from "../../lib/api";

export function OrganizationDetailPage() {
  const { organizationId = "" } = useParams();
  const [organization, setOrganization] = useState<OrganizationResponse>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  useEffect(() => {
    adminApi
      .organizationDetail(organizationId)
      .then(setOrganization)
      .catch((err: Error) => setError(`Organization could not be loaded: ${err.message}`))
      .finally(() => setLoading(false));
  }, [organizationId]);

  return (
    <section className="page">
      <PageHeader
        eyebrow="Organization"
        title={organization?.name ?? organizationId}
        description="Tenant profile, admins, members, invitations, roles, and settings summary."
        actions={
          <>
            <Link className="button-secondary" to={`/organizations/${organizationId}/members`}>Members</Link>
            <Link className="button-secondary" to={`/organizations/${organizationId}/invitations`}>Invitations</Link>
            <Link className="button-secondary" to={`/organizations/${organizationId}/security`}>Security</Link>
            <Link className="button-primary" to={`/audit/logs?organizationId=${organizationId}`}>Audit events</Link>
          </>
        }
      />
      {loading && <LoadingState label="Loading organization" />}
      {error && <ErrorState message={error} />}
      {organization && (
        <div className="detail-summary">
          <EntitySummaryCard label="Slug" value={organization.slug ?? "-"} />
          <EntitySummaryCard label="Status" value={<StatusBadge status={organization.status ?? "Unknown"} />} />
          <EntitySummaryCard label="Members" value={organization.memberCount ?? 0} />
          <EntitySummaryCard label="Pending invites" value={organization.invitationsCount ?? 0} />
        </div>
      )}
      <section className="panel">
        <h2>Settings Summary</h2>
        <p className="muted">{organization?.settingsSummary ?? "Security policy, branding, IdP, and notification settings will appear here as admin APIs mature."}</p>
      </section>
    </section>
  );
}
