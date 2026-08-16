import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { DataTable } from "../../components/ui/DataTable";
import { ErrorState } from "../../components/ui/ErrorState";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { Pagination } from "../../components/ui/Pagination";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { adminApi, type OrganizationMemberResponse, type PageResponse } from "../../lib/api";

const pageSize = 20;

export function OrganizationMembersPage() {
  const { organizationId = "" } = useParams();
  const [page, setPage] = useState(0);
  const [members, setMembers] = useState<PageResponse<OrganizationMemberResponse>>({
    content: [],
    page: 0,
    size: pageSize,
    totalElements: 0,
    totalPages: 0,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  useEffect(() => {
    setLoading(true);
    adminApi
      .organizationMembers(organizationId, { page, size: pageSize })
      .then(setMembers)
      .catch((err: Error) => setError(`Members could not be loaded: ${err.message}`))
      .finally(() => setLoading(false));
  }, [organizationId, page]);

  return (
    <section className="page">
      <PageHeader
        eyebrow="Organization"
        title="Members"
        description="Add members, change organization roles, and remove memberships."
        actions={<button className="button-primary">Add member</button>}
      />
      {loading && <LoadingState label="Loading members" />}
      {error && <ErrorState message={error} />}
      <DataTable
        rows={members.content as Record<string, unknown>[]}
        fallback="No members returned from the admin API."
        columns={[
          { key: "fullName", label: "Name", render: (row) => String(row.fullName ?? row.email ?? row.userId ?? "-") },
          { key: "email", label: "Email" },
          { key: "orgRole", label: "Org role" },
          { key: "status", label: "Status", render: (row) => <StatusBadge status={String(row.status ?? "Active")} /> },
          { key: "joinedAt", label: "Joined" },
        ]}
      />
      <Pagination page={page} size={pageSize} totalElements={members.totalElements} onPageChange={setPage} />
    </section>
  );
}
