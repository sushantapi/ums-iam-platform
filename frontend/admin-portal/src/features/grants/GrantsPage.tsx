import { useEffect, useState } from "react";
import { DataTable } from "../../components/ui/DataTable";
import { ErrorState } from "../../components/ui/ErrorState";
import { FilterBar } from "../../components/ui/FilterBar";
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
  const [userId, setUserId] = useState("");
  const [roleId, setRoleId] = useState("");
  const [organizationId, setOrganizationId] = useState("");
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
    adminApi
      .grants({ page, size: pageSize, userId, roleId, organizationId })
      .then(setGrants)
      .catch((err: Error) => setError(`Grants could not be loaded: ${err.message}`))
      .finally(() => setLoading(false));
  }, [organizationId, page, roleId, userId]);

  async function revoke(grantId: string) {
    try {
      await adminApi.revokeGrant(grantId);
      setGrants((current) => ({
        ...current,
        content: current.content.map((grant) =>
          grant.id === grantId ? { ...grant, status: "REVOKED" } : grant,
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
        description="Admin-wide entitlement inventory showing who has what role, in which tenant, and why."
      />
      <FilterBar>
        <label>
          User
          <input value={userId} onChange={(event) => { setPage(0); setUserId(event.target.value); }} />
        </label>
        <label>
          Role
          <input value={roleId} onChange={(event) => { setPage(0); setRoleId(event.target.value); }} />
        </label>
        <label>
          Organization
          <input value={organizationId} onChange={(event) => { setPage(0); setOrganizationId(event.target.value); }} />
        </label>
      </FilterBar>
      {loading && <LoadingState label="Loading grants" />}
      {error && <ErrorState message={error} />}
      <DataTable
        rows={grants.content as Record<string, unknown>[]}
        fallback="No grants returned from the admin API."
        columns={[
          { key: "principal", label: "Principal", render: (row) => String(row.principal ?? row.principalId ?? "-") },
          { key: "principalType", label: "Type" },
          { key: "roleName", label: "Role" },
          { key: "organizationName", label: "Organization", render: (row) => String(row.organizationName ?? row.organizationId ?? "-") },
          { key: "scope", label: "Scope" },
          { key: "assignedBy", label: "Assigned by" },
          { key: "assignedAt", label: "Assigned at" },
          { key: "status", label: "Status", render: (row) => <StatusBadge status={String(row.status ?? "Active")} /> },
          {
            key: "actions",
            label: "Actions",
            render: (row) => (
              <button className="inline-action" type="button" disabled={!canManageRoles} onClick={() => void revoke(String(row.id))}>
                Revoke
              </button>
            ),
          },
        ]}
      />
      <Pagination page={page} size={pageSize} totalElements={grants.totalElements} onPageChange={setPage} />
    </section>
  );
}
