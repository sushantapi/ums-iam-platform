import { useEffect, useState } from "react";
import { DataTable } from "../../components/ui/DataTable";
import { ErrorState } from "../../components/ui/ErrorState";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { Pagination } from "../../components/ui/Pagination";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { adminApi, type GrantResponse, type PageResponse } from "../../lib/api";
import { hasAdminCapability } from "../../lib/auth/capabilities";

const pageSize = 20;

export function GrantsPage() {
  const canManageRoles = hasAdminCapability("roles.manage");
  const [page, setPage] = useState(0);
  const [grants, setGrants] = useState<PageResponse<GrantResponse>>({
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
    setError(undefined);

    adminApi
      .grants({ page, size: pageSize })
      .then(setGrants)
      .catch((err: Error) =>
        setError(`Grants could not be loaded: ${err.message}`),
      )
      .finally(() => setLoading(false));
  }, [page]);

  async function revoke(assignmentId: string) {
    try {
      await adminApi.revokeGrant(assignmentId);

      setGrants((current) => ({
        ...current,
        content: current.content.map((grant) =>
          grant.assignmentId === assignmentId
            ? { ...grant, active: false }
            : grant,
        ),
      }));
    } catch (err) {
      setError(`Grant revoke failed: ${(err as Error).message}`);
    }
  }

  return (
    <section className="page">
      <PageHeader
        eyebrow="Authorization"
        title="Access Grants"
        description="Role assignment inventory reported by the admin API."
      />

      {loading && <LoadingState label="Loading grants" />}
      {error && <ErrorState message={error} />}

      <DataTable
        rows={grants.content as Record<string, unknown>[]}
        fallback="No grants returned from the admin API."
        columns={[
          { key: "assignmentId", label: "Assignment ID" },
          { key: "roleName", label: "Role" },
          { key: "scopeType", label: "Scope type" },
          { key: "scopeId", label: "Scope ID" },
          { key: "assignedAt", label: "Assigned at" },
          {
            key: "expiresAt",
            label: "Expires at",
            render: (row) => String(row.expiresAt ?? "Never"),
          },
          {
            key: "active",
            label: "Status",
            render: (row) => (
              <StatusBadge
                status={row.active === false ? "Inactive" : "Active"}
              />
            ),
          },
          {
            key: "actions",
            label: "Actions",
            render: (row) => (
              <button
                className="inline-action"
                type="button"
                disabled={!canManageRoles || row.active === false}
                onClick={() => void revoke(String(row.assignmentId))}
              >
                Revoke
              </button>
            ),
          },
        ]}
      />

      <Pagination
        page={page}
        size={pageSize}
        totalElements={grants.totalElements}
        onPageChange={setPage}
      />
    </section>
  );
}
