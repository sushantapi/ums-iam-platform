import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { DataTable } from "../../components/ui/DataTable";
import { ErrorState } from "../../components/ui/ErrorState";
import { FilterBar } from "../../components/ui/FilterBar";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { Pagination } from "../../components/ui/Pagination";
import { adminApi, type PageResponse, type UserSummaryResponse } from "../../lib/api";
import { hasAdminCapability } from "../../lib/auth/capabilities";
import { useAdminListState } from "../../lib/hooks/useAdminListState";

const pageSize = 20;
const defaultFilters = { search: "" };

export function UsersPage() {
  const navigate = useNavigate();
  const canManageUsers = hasAdminCapability("users.manage");
  const { page, filters, setFilter, setPage } = useAdminListState(defaultFilters, pageSize);
  const { search } = filters;

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
    setError(undefined);

    adminApi
      .users({ page, size: pageSize, search })
      .then(setUsers)
      .catch((err: Error) => setError(`Users could not be loaded: ${err.message}`))
      .finally(() => setLoading(false));
  }, [page, search]);

  async function refreshUsers() {
    const refreshed = await adminApi.users({
      page,
      size: pageSize,
      search,
    });

    setUsers(refreshed);
  }

  async function runUserAction(userId: string, action: string) {
    setError(undefined);

    try {
      if (action === "assign-role") {
        navigate("/roles/assignments");
        return;
      }

      if (action === "audit") {
        navigate(`/audit/logs?target=${userId}`);
        return;
      }

      if (action === "activate") {
        await adminApi.activateUser(userId);
      }

      if (action === "suspend") {
        await adminApi.suspendUser(userId);
      }

      if (action === "unlock") {
        await adminApi.unlockUser(userId);
      }

      await refreshUsers();
    } catch (err) {
      setError(`Action failed: ${(err as Error).message}`);
    }
  }

  return (
    <section className="page">
      <PageHeader
        eyebrow="Directory"
        title="Users"
        description="Search, inspect, and administer supported user lifecycle actions across the IAM platform."
      />

      <FilterBar>
        <label>
          Search
          <input
            value={search}
            placeholder="Name or email"
            onChange={(event) => setFilter("search", event.target.value)}
          />
        </label>
      </FilterBar>

      {loading && <LoadingState label="Loading users" />}
      {error && <ErrorState message={error} />}

      <DataTable
        rows={users.content as Record<string, unknown>[]}
        fallback="No users returned from the admin API."
        onRowClick={(row) => navigate(`/users/${String(row.id ?? row.userId)}`)}
        columns={[
          {
            key: "id",
            label: "ID",
            render: (row) => String(row.id ?? row.userId ?? "-"),
          },
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
          {
            key: "email",
            label: "Email",
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

                    if (action) {
                      void runUserAction(userId, action);
                    }
                  }}
                >
                  <option value="">Choose action</option>
                  <option value="activate" disabled={!canManageUsers}>
                    Activate
                  </option>
                  <option value="suspend" disabled={!canManageUsers}>
                    Suspend
                  </option>
                  <option value="unlock" disabled={!canManageUsers}>
                    Unlock
                  </option>
                  <option value="assign-role" disabled={!canManageUsers}>
                    Assign role
                  </option>
                  <option value="audit">View audit trail</option>
                </select>
              );
            },
          },
        ]}
      />

      <Pagination
        page={page}
        size={pageSize}
        totalElements={users.totalElements}
        onPageChange={setPage}
      />
    </section>
  );
}
