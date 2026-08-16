import { FormEvent, useEffect, useState } from "react";
import { DataTable } from "../../components/ui/DataTable";
import { ErrorState } from "../../components/ui/ErrorState";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { StatusBadge } from "../../components/ui/StatusBadge";
import {
  adminApi,
  type PermissionResponse,
  type RoleResponse,
} from "../../lib/api";

type ScopeType = "PLATFORM" | "ORG" | "DEPARTMENT";

const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export function RoleAssignmentsPage() {
  const [userId, setUserId] = useState("");
  const [roleName, setRoleName] = useState("");
  const [scopeType, setScopeType] = useState<ScopeType>("PLATFORM");
  const [scopeId, setScopeId] = useState("*");

  const [roles, setRoles] = useState<RoleResponse[]>([]);
  const [selectedRole, setSelectedRole] = useState<RoleResponse>();
  const [permissions, setPermissions] = useState<PermissionResponse[]>([]);

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string>();
  const [message, setMessage] = useState<string>();

  useEffect(() => {
    adminApi
      .roles()
      .then((payload) => {
        const items = payload;

        setRoles(items);

        const firstRole = items[0];
        setSelectedRole(firstRole);

        if (firstRole?.name) {
          setRoleName(firstRole.name);
        }

        if (firstRole?.id) {
          void adminApi
            .rolePermissions(String(firstRole.id))
            .then(setPermissions)
            .catch(() => setPermissions([]));
        }
      })
      .catch((err: Error) =>
        setError(`Roles could not be loaded: ${err.message}`),
      )
      .finally(() => setLoading(false));
  }, []);

  async function selectRole(role: RoleResponse) {
    setSelectedRole(role);
    setRoleName(role.name ?? "");

    if (!role.id) {
      setPermissions([]);
      return;
    }

    try {
      setPermissions(
        await adminApi.rolePermissions(String(role.id)),
      );
    } catch {
      setPermissions([]);
    }
  }

  function changeScope(nextScope: ScopeType) {
    setScopeType(nextScope);

    if (nextScope === "PLATFORM") {
      setScopeId("*");
    } else {
      setScopeId("");
    }
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage(undefined);
    setError(undefined);

    if (!UUID_PATTERN.test(userId.trim())) {
      setError("User ID must be a valid UUID.");
      return;
    }

    if (!roleName) {
      setError("Select a role before assigning access.");
      return;
    }

    if (scopeType !== "PLATFORM" && !scopeId.trim()) {
      setError(`${scopeType} scope requires a scope ID.`);
      return;
    }

    setSubmitting(true);

    try {
      await adminApi.assignRole({
        userId: userId.trim(),
        roleName,
        scopeType,
        scopeId: scopeType === "PLATFORM" ? "*" : scopeId.trim(),
      });

      setMessage(
        `${roleName} assigned with ${scopeType} scope.`,
      );
    } catch (err) {
      setError(
        `Role assignment failed: ${(err as Error).message}`,
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="page">
      <PageHeader
        eyebrow="Access Control"
        title="Role Assignments"
        description="Review role permissions and assign platform, organization, or department scoped access."
      />

      {loading && <LoadingState label="Loading roles" />}
      {error && <ErrorState message={error} />}
      {message && <div className="notice">{message}</div>}

      <div className="entitlement-layout">
        <section className="panel panel-wide">
          <h2>Role List</h2>

          <DataTable
            rows={roles as Record<string, unknown>[]}
            fallback="No roles returned from the admin API."
            onRowClick={(row) =>
              void selectRole(row as RoleResponse)
            }
            columns={[
              {
                key: "name",
                label: "Role",
                render: (row) =>
                  String(row.name ?? "-"),
              },
              {
                key: "description",
                label: "Description",
                render: (row) =>
                  String(row.description ?? "-"),
              },
              {
                key: "system",
                label: "Type",
                render: (row) => (
                  <StatusBadge
                    status={
                      row.system
                        ? "System"
                        : "Custom"
                    }
                  />
                ),
              },
              {
                key: "active",
                label: "Status",
                render: (row) => (
                  <StatusBadge
                    status={
                      row.active === false
                        ? "Inactive"
                        : "Active"
                    }
                  />
                ),
              },
            ]}
          />
        </section>

        <section className="panel">
          <h2>Role Detail</h2>

          <ul className="detail-list">
            <li>
              Name: {selectedRole?.name ?? "Select a role"}
            </li>
            <li>
              Description:{" "}
              {selectedRole?.description ?? "Not reported"}
            </li>
            <li>
              Type:{" "}
              {selectedRole
                ? selectedRole.system
                  ? "System role"
                  : "Custom role"
                : "Not reported"}
            </li>
            <li>
              Status:{" "}
              {selectedRole
                ? selectedRole.active === false
                  ? "Inactive"
                  : "Active"
                : "Not reported"}
            </li>
          </ul>

          <h2>Permissions Included</h2>

          <ul className="detail-list">
            {permissions.map((permission) => (
              <li key={permission.id ?? permission.code}>
                {permission.code ?? "Permission"}
                {permission.action
                  ? ` - ${permission.action}`
                  : ""}
                {permission.description
                  ? ` - ${permission.description}`
                  : ""}
              </li>
            ))}

            {permissions.length === 0 && (
              <li>
                No permissions returned for the selected role.
              </li>
            )}
          </ul>
        </section>

        <form className="form-panel" onSubmit={submit}>
          <h2>Assignment Panel</h2>

          <label>
            User ID
            <input
              value={userId}
              onChange={(event) =>
                setUserId(event.target.value)
              }
              placeholder="User UUID"
              required
            />
          </label>

          <label>
            Role
            <select
              value={roleName}
              onChange={(event) => {
                const role = roles.find(
                  (item) =>
                    item.name === event.target.value,
                );

                if (role) {
                  void selectRole(role);
                }
              }}
              required
            >
              <option value="" disabled>
                Select role
              </option>

              {roles.map((role) => (
                <option
                  key={role.id ?? role.name}
                  value={role.name ?? ""}
                >
                  {role.name}
                </option>
              ))}
            </select>
          </label>

          <label>
            Scope type
            <select
              value={scopeType}
              onChange={(event) =>
                changeScope(
                  event.target.value as ScopeType,
                )
              }
            >
              <option value="PLATFORM">
                Platform
              </option>
              <option value="ORG">
                Organization
              </option>
              <option value="DEPARTMENT">
                Department
              </option>
            </select>
          </label>

          <label>
            Scope ID
            <input
              value={scopeId}
              onChange={(event) =>
                setScopeId(event.target.value)
              }
              placeholder={
                scopeType === "PLATFORM"
                  ? "*"
                  : "Scope UUID"
              }
              disabled={scopeType === "PLATFORM"}
              required={scopeType !== "PLATFORM"}
              maxLength={36}
            />
          </label>

          <button
            className="button-primary"
            type="submit"
            disabled={submitting || loading}
          >
            {submitting
              ? "Assigning..."
              : "Assign role"}
          </button>
        </form>
      </div>
    </section>
  );
}
