import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { DataTable } from "../../components/ui/DataTable";
import { ErrorState } from "../../components/ui/ErrorState";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { adminApi, type OrganizationMemberResponse } from "../../lib/api";

export function OrganizationMembersPage() {
  const { organizationId = "" } = useParams();
  const [members, setMembers] = useState<OrganizationMemberResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  useEffect(() => {
    setLoading(true);
    setError(undefined);

    adminApi
      .organizationMembers(organizationId)
      .then(setMembers)
      .catch((err: Error) =>
        setError(`Members could not be loaded: ${err.message}`),
      )
      .finally(() => setLoading(false));
  }, [organizationId]);

  return (
    <section className="page">
      <PageHeader
        eyebrow="Organization"
        title="Members"
        description="Organization memberships reported by the admin API."
      />

      {loading && <LoadingState label="Loading members" />}
      {error && <ErrorState message={error} />}

      <DataTable
        rows={members as Record<string, unknown>[]}
        fallback="No members returned from the admin API."
        columns={[
          { key: "id", label: "Membership ID" },
          { key: "userId", label: "User ID" },
          { key: "role", label: "Role" },
          { key: "joinedAt", label: "Joined at" },
        ]}
      />
    </section>
  );
}
