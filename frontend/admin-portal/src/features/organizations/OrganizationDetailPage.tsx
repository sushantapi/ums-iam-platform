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
  const [organization, setOrganization] =
    useState<OrganizationResponse>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  useEffect(() => {
    setLoading(true);
    setError(undefined);

    adminApi
      .organizationDetail(organizationId)
      .then(setOrganization)
      .catch((err: Error) =>
        setError(`Organization could not be loaded: ${err.message}`),
      )
      .finally(() => setLoading(false));
  }, [organizationId]);

  return (
    <section className="page">
      <PageHeader
        eyebrow="Organization"
        title={organization?.name ?? organizationId}
        description="Organization profile reported by the admin API."
        actions={
          <div className="action-row">
            <Link
              className="button-secondary"
              to={`/organizations/${organizationId}/members`}
            >
              Members
            </Link>
            <Link
              className="button-secondary"
              to={`/organizations/${organizationId}/security`}
            >
              Security
            </Link>
          </div>
        }
      />

      {loading && <LoadingState label="Loading organization" />}
      {error && <ErrorState message={error} />}

      {organization && (
        <>
          <div className="detail-summary">
            <EntitySummaryCard
              label="Slug"
              value={organization.slug}
            />
            <EntitySummaryCard
              label="Status"
              value={<StatusBadge status={organization.status} />}
            />
            <EntitySummaryCard
              label="Owner ID"
              value={organization.ownerId ?? "-"}
            />
            <EntitySummaryCard
              label="Organization ID"
              value={organization.id}
            />
          </div>

          <section className="panel">
            <h2>Description</h2>
            <p className="muted">
              {organization.description || "No description provided."}
            </p>
          </section>
        </>
      )}
    </section>
  );
}
