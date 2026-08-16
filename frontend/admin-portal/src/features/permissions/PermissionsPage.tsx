import { useEffect, useState } from "react";
import { DataTable } from "../../components/ui/DataTable";
import { ErrorState } from "../../components/ui/ErrorState";
import { FilterBar } from "../../components/ui/FilterBar";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { Pagination } from "../../components/ui/Pagination";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { adminApi, type PageResponse, type PermissionResponse } from "../../lib/api";

const pageSize = 20;

export function PermissionsPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [permissions, setPermissions] = useState<PageResponse<PermissionResponse>>({
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
      .permissions({ page, size: pageSize, search })
      .then(setPermissions)
      .catch((err: Error) => setError(`Permissions could not be loaded: ${err.message}`))
      .finally(() => setLoading(false));
  }, [page, search]);

  return (
    <section className="page">
      <PageHeader
        eyebrow="Authorization"
        title="Permissions"
        description="Catalog of resource-action permissions and their role usage."
      />
      <FilterBar>
        <label>
          Search
          <input value={search} placeholder="permission, resource, action" onChange={(event) => { setPage(0); setSearch(event.target.value); }} />
        </label>
      </FilterBar>
      {loading && <LoadingState label="Loading permissions" />}
      {error && <ErrorState message={error} />}
      <DataTable
        rows={permissions.content as Record<string, unknown>[]}
        fallback="No permissions returned from the admin API."
        columns={[
          { key: "code", label: "Permission code" },
          { key: "resource", label: "Resource" },
          { key: "action", label: "Action" },
          { key: "description", label: "Description" },
          { key: "systemPermission", label: "Type", render: (row) => <StatusBadge status={row.systemPermission ? "System" : "Custom"} /> },
          { key: "rolesUsingPermission", label: "Roles using", render: (row) => String(row.rolesUsingPermission ?? 0) },
        ]}
      />
      <Pagination page={page} size={pageSize} totalElements={permissions.totalElements} onPageChange={setPage} />
    </section>
  );
}
