import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { DataTable } from "../../components/ui/DataTable";
import { EntitySummaryCard } from "../../components/ui/EntitySummaryCard";
import { ErrorState } from "../../components/ui/ErrorState";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { adminApi, type GrantResponse, type PermissionResponse, type RoleResponse } from "../../lib/api";

export function RoleDetailPage() {
  const { roleId = "" } = useParams();
  const [role, setRole] = useState<RoleResponse>();
  const [permissions, setPermissions] = useState<PermissionResponse[]>([]);
  const [assignments, setAssignments] = useState<GrantResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  useEffect(() => {
    Promise.allSettled([
      adminApi.roleDetail(roleId),
      adminApi.rolePermissions(roleId),
      adminApi.roleAssignments(roleId, { page: 0, size: 20 }),
    ])
      .then(([roleResult, permissionsResult, assignmentsResult]) => {
        if (roleResult.status === "fulfilled") setRole(roleResult.value);
        if (permissionsResult.status === "fulfilled") setPermissions(permissionsResult.value);
        if (assignmentsResult.status === "fulfilled") setAssignments(assignmentsResult.value.content);
        if (roleResult.status === "rejected") setError(`Role could not be loaded: ${String(roleResult.reason)}`);
      })
      .finally(() => setLoading(false));
  }, [roleId]);

  return (
    <section className="page">
      <PageHeader
        eyebrow="Authorization"
        title={role?.name ?? role?.roleName ?? roleId}
        description="Role metadata, permission membership, assignment count, scope, and ownership."
        actions={<Link className="button-secondary" to="/roles">Back to roles</Link>}
      />
      {loading && <LoadingState label="Loading role" />}
      {error && <ErrorState message={error} />}
      <div className="detail-summary">
        <EntitySummaryCard label="Scope" value={<StatusBadge status={role?.scopeType ?? "Unknown"} />} />
        <EntitySummaryCard label="Permissions" value={permissions.length} />
        <EntitySummaryCard label="Assignments" value={assignments.length} />
        <EntitySummaryCard label="Type" value={role?.systemRole ? "System" : "Custom"} />
      </div>
      <section className="panel">
        <h2>Permissions</h2>
        <DataTable
          rows={permissions as Record<string, unknown>[]}
          fallback="No permissions returned for this role."
          columns={[
            { key: "code", label: "Code" },
            { key: "resource", label: "Resource" },
            { key: "action", label: "Action" },
            { key: "description", label: "Description" },
          ]}
        />
      </section>
      <section className="panel stacked-panel">
        <h2>Assignments</h2>
        <DataTable
          rows={assignments as Record<string, unknown>[]}
          fallback="No assignments returned for this role."
          columns={[
            { key: "principal", label: "Principal", render: (row) => String(row.principal ?? row.principalId ?? "-") },
            { key: "organizationName", label: "Organization", render: (row) => String(row.organizationName ?? row.organizationId ?? "-") },
            { key: "assignedBy", label: "Assigned by" },
            { key: "assignedAt", label: "Assigned at" },
            { key: "status", label: "Status", render: (row) => <StatusBadge status={String(row.status ?? "Active")} /> },
          ]}
        />
      </section>
    </section>
  );
}
