import { FormEvent, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import userAdminService from "../../api/services/userAdminService";
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
  const [message, setMessage] = useState<string>();
  const [showCreate, setShowCreate] = useState(false);
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [creating, setCreating] = useState(false);

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

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(undefined);
    setMessage(undefined);
    setCreating(true);

    try {
      const created = await userAdminService.create({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim().toLowerCase(),
        password,
      });

      setMessage(`User created: ${created.email} (${created.userId}).`);
      setFirstName("");
      setLastName("");
      setEmail("");
      setPassword("");
      setShowCreate(false);

      try {
        await refreshUsers();
      } catch {
        // User profile projection is event-driven and may appear shortly after creation.
      }
    } catch (err) {
      setError(`User could not be created: ${(err as Error).message}`);
    } finally {
      setCreating(false);
    }
  }

  async function runUserAction(userId: string, action: string) {
    setError(undefined);

    try {
      if (action === "assign-role") {
        navigate(`/roles/assignments?userId=${encodeURIComponent(userId)}`);
        return;
      }

      if (action === "audit") {
        navigate(`/audit/logs?target=${encodeURIComponent(userId)}`);
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
        description="Create, search, inspect, and administer users across the IAM platform."
        actions={
          canManageUsers ? (
            <button
              type="button"
              className="button-primary"
              onClick={() => setShowCreate((current) => !current)}
            >
              {showCreate ? "Cancel" : "Create user"}
            </button>
          ) : undefined
        }
      />

      {showCreate && canManageUsers ? (
        <section className="panel">
          <h2>Create user</h2>
          <form onSubmit={handleCreate}>
            <label>
              First name
              <input
                value={firstName}
                onChange={(event) => setFirstName(event.target.value)}
                disabled={creating}
                required
              />
            </label>

            <label>
              Last name
              <input
                value={lastName}
                onChange={(event) => setLastName(event.target.value)}
                disabled={creating}
                required
              />
            </label>

            <label>
              Email
              <input
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                disabled={creating}
                required
              />
            </label>

            <label>
              Initial password
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                disabled={creating}
                minLength={8}
                autoComplete="new-password"
                required
              />
            </label>

            <p className="muted">
              Minimum 8 characters with uppercase, lowercase, digit, and special character.
            </p>

            <button
              type="submit"
              className="button-primary"
              disabled={creating}
            >
              {creating ? "Creating..." : "Create user"}
            </button>
          </form>
        </section>
      ) : null}

      {message && <div className="notice">{message}</div>}

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
        onRowClick={(row) => navigate(`/users/${String(row.id)}`)}
        columns={[
          {
            key: "id",
            label: "ID",
            render: (row) => String(row.id),
          },
          {
            key: "fullName",
            label: "Name",
            render: (row) =>
              [row.firstName, row.lastName].filter(Boolean).join(" ") || "-",
          },
          {
            key: "email",
            label: "Email",
          },
          {
            key: "actions",
            label: "Actions",
            render: (row) => {
              const userId = String(row.id);

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
