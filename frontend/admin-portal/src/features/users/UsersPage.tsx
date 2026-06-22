import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { DataTable } from "../../components/ui/DataTable";
import { ErrorState } from "../../components/ui/ErrorState";
import { FilterBar } from "../../components/ui/FilterBar";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { Pagination } from "../../components/ui/Pagination";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { adminApi, type PageResponse, type UserSummaryResponse } from "../../lib/api";
import { hasAdminCapability } from "../../lib/auth/capabilities";
import { useAdminListState } from "../../lib/hooks/useAdminListState";

const pageSize = 20;
const defaultFilters = { search: "", status: "", organizationId: "", role: "" };

export function UsersPage() {
  const navigate = useNavigate();
  const canManageUsers = hasAdminCapability("users.manage");
  const { page, filters, setFilter, setPage } = useAdminListState(defaultFilters, pageSize);
  const { search, status, organizationId, role } = filters;
  const [users, setUsers] = useState<PageResponse<UserSummaryResponse>>({
    content: [],
    totalElements: 0,
    totalPages: 0,
    page: 0,
    size: pageSize,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  useEffect(() => {
    setLoading(true);
    adminApi
      .users({ page, size: pageSize, search, status, organizationId, role })
      .then(setUsers)
      .catch((err: Error) => setError(`Users could not be loaded: ${err.message}`))
      .finally(() => setLoading(false));
  }, [organizationId, page, role, search, status]);

  async function runUserAction(userId: string, action: string) {
    setError(undefined);
    try {
      if (action === "activate") await adminApi.activateUser(userId);
      if (action === "suspend") await adminApi.suspendUser(userId);
      if (action === "unlock") await adminApi.unlockUser(userId);
      if (action === "reset-password") await adminApi.resetPassword(userId);
      if (action === "assign-role") navigate("/roles/assignments");
      if (action === "audit") navigate(`/audit/logs?target=${userId}`);
      if (!["assign-role", "audit"].includes(action)) {
        const refreshed = await adminApi.users({ page, size: pageSize, search, status, organizationId, role });
        setUsers(refreshed);
      }
    } catch (err) {
      setError(`Action failed: ${(err as Error).message}`);
    }
  }

  return (
    <section className="page">
      <PageHeader
        eyebrow="Directory"
        title="Users"
        description="Search, filter, inspect, and administer user lifecycle state across the IAM platform."
        actions={
          <>
          <button className="button-secondary" disabled={!canManageUsers}>Bulk import</button>
          <button className="button-primary" disabled={!canManageUsers}>Create user</button>
          </>
        }
      />
      <FilterBar>
        <label>
          Search
          <input value={search} placeholder="Name or email" onChange={(event) => setFilter("search", event.target.value)} />
        </label>
        <label>
          Status
          <select value={status} onChange={(event) => setFilter("status", event.target.value)}>
            <option value="">All</option>
            <option value="ACTIVE">Active</option>
            <option value="LOCKED">Locked</option>
            <option value="SUSPENDED">Suspended</option>
            <option value="DISABLED">Disabled</option>
          </select>
        </label>
        <label>
          Organization
          <input value={organizationId} placeholder="Organization ID" onChange={(event) => setFilter("organizationId", event.target.value)} />
        </label>
        <label>
          Role
          <input value={role} placeholder="Role name" onChange={(event) => setFilter("role", event.target.value)} />
        </label>
      </FilterBar>
      {loading && <LoadingState label="Loading users" />}
      {error && <ErrorState message={error} />}
      <DataTable
        rows={users.content as Record<string, unknown>[]}
        fallback="No users returned from the admin API."
        onRowClick={(row) => navigate(`/users/${String(row.id ?? row.userId)}`)}
        columns={[
          { key: "id", label: "ID", render: (row) => String(row.id ?? row.userId ?? "-") },
          {
            key: "fullName",
            label: "Name",
            render: (row) =>
              String(
                row.fullName ||
                  [row.firstName, row.lastName].filter(Boolean).join(" ") ||
                  row.username ||
                  "-",
              ),
          },
          { key: "email", label: "Email" },
          { key: "organizationName", label: "Organization", render: (row) => String(row.organizationName ?? row.organizationId ?? "-") },
          {
            key: "status",
            label: "Status",
            render: (row) => <StatusBadge status={String(row.status ?? "Unknown")} />,
          },
          {
            key: "roles",
            label: "Roles",
            render: (row) => (Array.isArray(row.roles) ? row.roles.join(", ") : "-"),
          },
          {
            key: "actions",
            label: "Actions",
            render: (row) => {
              const userId = String(row.id ?? row.userId ?? "");
              return (
                <select
                  className="table-action"
                  defaultValue=""
                  onClick={(event) => event.stopPropagation()}
                  onChange={(event) => {
                    const action = event.target.value;
                    event.target.value = "";
                    if (action) void runUserAction(userId, action);
                  }}
                >
                  <option value="">Choose action</option>
                  <option value="activate" disabled={!canManageUsers}>Activate</option>
                  <option value="suspend" disabled={!canManageUsers}>Suspend</option>
                  <option value="unlock" disabled={!canManageUsers}>Lock / unlock</option>
                  <option value="reset-password" disabled={!canManageUsers}>Reset password</option>
                  <option value="assign-role" disabled={!canManageUsers}>Assign role</option>
                  <option value="audit">View audit trail</option>
                </select>
              );
            },
          },
        ]}
      />
      <Pagination page={page} size={pageSize} totalElements={users.totalElements} onPageChange={setPage} />
    </section>
  );
}
