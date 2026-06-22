import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { DataTable } from "../../components/ui/DataTable";
import { ErrorState } from "../../components/ui/ErrorState";
import { FilterBar } from "../../components/ui/FilterBar";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { Pagination } from "../../components/ui/Pagination";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { adminApi, type PageResponse, type RoleResponse } from "../../lib/api";

const pageSize = 20;

export function RolesPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [scopeType, setScopeType] = useState("");
  const [search, setSearch] = useState("");
  const [roles, setRoles] = useState<PageResponse<RoleResponse>>({
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
      .roles({ page, size: pageSize, scopeType, search })
      .then((payload) => {
        setRoles(Array.isArray(payload) ? {
          content: payload,
          page,
          size: pageSize,
          totalElements: payload.length,
          totalPages: payload.length > 0 ? 1 : 0,
        } : payload);
      })
      .catch((err: Error) => setError(`Roles could not be loaded: ${err.message}`))
      .finally(() => setLoading(false));
  }, [page, scopeType, search]);

  return (
    <section className="page">
      <PageHeader
        eyebrow="Authorization"
        title="Roles"
        description="Role catalog with scope, permissions, assignment count, and system/custom ownership."
        actions={<button className="button-primary">Create role</button>}
      />
      <FilterBar>
        <label>
          Search
          <input value={search} onChange={(event) => { setPage(0); setSearch(event.target.value); }} />
        </label>
        <label>
          Scope type
          <select value={scopeType} onChange={(event) => { setPage(0); setScopeType(event.target.value); }}>
            <option value="">All</option>
            <option value="platform">Platform</option>
            <option value="organization">Organization</option>
          </select>
        </label>
      </FilterBar>
      {loading && <LoadingState label="Loading roles" />}
      {error && <ErrorState message={error} />}
      <DataTable
        rows={roles.content as Record<string, unknown>[]}
        fallback="No roles returned from the admin API."
        onRowClick={(row) => navigate(`/roles/${String(row.id ?? row.roleId)}`)}
        columns={[
          { key: "name", label: "Role", render: (row) => String(row.name ?? row.roleName ?? "-") },
          { key: "scopeType", label: "Scope", render: (row) => <StatusBadge status={String(row.scopeType ?? "platform")} /> },
          { key: "permissionCount", label: "Permissions", render: (row) => String(row.permissionCount ?? 0) },
          { key: "assignedUserCount", label: "Assignments", render: (row) => String(row.assignedUserCount ?? 0) },
          { key: "systemRole", label: "Type", render: (row) => <StatusBadge status={row.systemRole ? "System" : "Custom"} /> },
        ]}
      />
      <Pagination page={page} size={pageSize} totalElements={roles.totalElements} onPageChange={setPage} />
    </section>
  );
}
