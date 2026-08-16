import { FormEvent, useEffect, useState } from "react";
import { DataTable } from "../../components/ui/DataTable";
import { ErrorState } from "../../components/ui/ErrorState";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { adminApi, type PermissionResponse, type RoleResponse } from "../../lib/api";

export function RoleAssignmentsPage() {
  const [userId, setUserId] = useState("");
  const [roleName, setRoleName] = useState("ADMIN");
  const [organizationId, setOrganizationId] = useState("");
  const [roles, setRoles] = useState<RoleResponse[]>([]);
  const [selectedRole, setSelectedRole] = useState<RoleResponse>();
  const [permissions, setPermissions] = useState<PermissionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();
  const [message, setMessage] = useState<string>();

  useEffect(() => {
    adminApi
      .roles()
      .then((payload) => {
        const items = Array.isArray(payload) ? payload : payload.content;
        setRoles(items);
        setSelectedRole(items[0]);
        const firstRoleId = items[0]?.id ?? items[0]?.roleId;
        if (firstRoleId) {
          void adminApi.rolePermissions(String(firstRoleId)).then(setPermissions).catch(() => setPermissions([]));
        }
      })
      .catch((err: Error) => setError(`Roles could not be loaded: ${err.message}`))
      .finally(() => setLoading(false));
  }, []);

  async function selectRole(role: RoleResponse) {
    setSelectedRole(role);
    setRoleName(role.name ?? role.roleName ?? roleName);
    const roleId = role.id ?? role.roleId;
    if (!roleId) return;
    try {
      setPermissions(await adminApi.rolePermissions(String(roleId)));
    } catch {
      setPermissions([]);
    }
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage(undefined);
    setError(undefined);
    try {
      await adminApi.assignRole({ userId, roleName });
      setMessage(
        organizationId
          ? `Role assignment request submitted for organization ${organizationId}.`
          : "Role assignment request submitted.",
      );
    } catch (error) {
      setError(`Role assignment failed: ${(error as Error).message}`);
    }
  }

  return (
    <section className="page">
      <PageHeader
        eyebrow="Access Control"
        title="Role Assignments"
        description="Review role scope, permissions, and assignment authority before granting access."
      />
      {loading && <LoadingState label="Loading roles" />}
      {error && <ErrorState message={error} />}
      {message && <div className="notice">{message}</div>}
      <div className="entitlement-layout">
        <section className="panel panel-wide">
          <h2>Role List</h2>
          <DataTable
            rows={roles as Record<string, unknown>[]}
            fallback="No roles returned yet. Wire GET /api/v1/roles to populate this catalog."
            onRowClick={(row) => void selectRole(row as RoleResponse)}
            columns={[
              { key: "name", label: "Role", render: (row) => String(row.name ?? row.roleName ?? "-") },
              { key: "scopeType", label: "Scope", render: (row) => <StatusBadge status={String(row.scopeType ?? "platform")} /> },
              { key: "description", label: "Description" },
              { key: "permissionCount", label: "Permissions", render: (row) => String(row.permissionCount ?? "-") },
              { key: "assignedUserCount", label: "Assigned users", render: (row) => String(row.assignedUserCount ?? "-") },
            ]}
          />
        </section>

        <section className="panel">
          <h2>Role Detail</h2>
          <ul className="detail-list">
            <li>Name: {selectedRole?.name ?? selectedRole?.roleName ?? "Select a role"}</li>
            <li>Scope: {selectedRole?.scopeType ?? "Not reported"}</li>
            <li>Type: {selectedRole?.systemRole ? "System role" : "Custom role"}</li>
            <li>Assignable by: {(selectedRole?.assignableBy ?? ["Admin"]).join(", ")}</li>
          </ul>
          <h2>Permissions Included</h2>
          <ul className="detail-list">
            {permissions.map((permission) => (
              <li key={permission.id ?? permission.code}>
                {permission.code ?? `${permission.resource ?? "resource"}:${permission.action ?? "action"}`}
              </li>
            ))}
            {permissions.length === 0 && <li>No permissions returned for the selected role.</li>}
          </ul>
        </section>

        <form className="form-panel" onSubmit={submit}>
          <h2>Assignment Panel</h2>
          <label>
            User ID
            <input value={userId} onChange={(event) => setUserId(event.target.value)} required />
          </label>
          <label>
            Role
            <input value={roleName} onChange={(event) => setRoleName(event.target.value)} required />
          </label>
          <label>
            Organization / tenant scope
            <input value={organizationId} placeholder="Optional tenant ID" onChange={(event) => setOrganizationId(event.target.value)} />
          </label>
          <div className="form-grid-two">
            <label>
              Effective from
              <input type="date" />
            </label>
            <label>
              Effective until
              <input type="date" />
            </label>
          </div>
          <button className="button-primary" type="submit">
            Assign role
          </button>
        </form>
      </div>
    </section>
  );
}
