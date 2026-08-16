import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { DataTable } from "../../components/ui/DataTable";
import { EntitySummaryCard } from "../../components/ui/EntitySummaryCard";
import { ErrorState } from "../../components/ui/ErrorState";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { StatusBadge } from "../../components/ui/StatusBadge";
import {
  adminApi,
  type PermissionResponse,
  type RoleResponse,
} from "../../lib/api";

export function RoleDetailPage() {
  const { roleId = "" } = useParams();

  const [role, setRole] = useState<RoleResponse>();
  const [permissions, setPermissions] = useState<PermissionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  useEffect(() => {
    setLoading(true);
    setError(undefined);

    Promise.allSettled([
      adminApi.roleDetail(roleId),
      adminApi.rolePermissions(roleId),
    ])
      .then(([roleResult, permissionsResult]) => {
        if (roleResult.status === "fulfilled") {
          setRole(roleResult.value);
        } else {
          setError(
            `Role could not be loaded: ${String(roleResult.reason)}`,
          );
        }

        if (permissionsResult.status === "fulfilled") {
          setPermissions(permissionsResult.value);
        }
      })
      .finally(() => setLoading(false));
  }, [roleId]);

  return (
    <section className="page">
      <PageHeader
        eyebrow="Authorization"
        title={role?.name ?? roleId}
        description="Role metadata and permission membership reported by the admin API."
        actions={
          <Link className="button-secondary" to="/roles">
            Back to roles
          </Link>
        }
      />

      {loading && <LoadingState label="Loading role" />}
      {error && <ErrorState message={error} />}

      {role && (
        <div className="detail-summary">
          <EntitySummaryCard
            label="Status"
            value={
              <StatusBadge
                status={
                  role.active === false
                    ? "Inactive"
                    : "Active"
                }
              />
            }
          />

          <EntitySummaryCard
            label="Type"
            value={role.system ? "System" : "Custom"}
          />

          <EntitySummaryCard
            label="Permissions"
            value={permissions.length}
          />

          <EntitySummaryCard
            label="Role ID"
            value={role.id ?? roleId}
          />
        </div>
      )}

      <section className="panel">
        <h2>Permissions</h2>

        <DataTable
          rows={permissions as Record<string, unknown>[]}
          fallback="No permissions returned for this role."
          columns={[
            {
              key: "code",
              label: "Code",
              render: (row) => String(row.code ?? "-"),
            },
            {
              key: "action",
              label: "Action",
              render: (row) => String(row.action ?? "-"),
            },
            {
              key: "description",
              label: "Description",
              render: (row) =>
                String(row.description ?? "-"),
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
    </section>
  );
}
