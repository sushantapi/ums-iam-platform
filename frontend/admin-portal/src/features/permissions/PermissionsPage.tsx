import { useEffect, useMemo, useState } from "react";
import { DataTable } from "../../components/ui/DataTable";
import { ErrorState } from "../../components/ui/ErrorState";
import { FilterBar } from "../../components/ui/FilterBar";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { adminApi, type PermissionResponse } from "../../lib/api";

export function PermissionsPage() {
  const [search, setSearch] = useState("");
  const [permissions, setPermissions] = useState<PermissionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  useEffect(() => {
    setLoading(true);
    setError(undefined);

    adminApi
      .permissions()
      .then(setPermissions)
      .catch((err: Error) =>
        setError(`Permissions could not be loaded: ${err.message}`),
      )
      .finally(() => setLoading(false));
  }, []);

  const filteredPermissions = useMemo(() => {
    const value = search.trim().toLowerCase();

    if (!value) {
      return permissions;
    }

    return permissions.filter((permission) =>
      [
        permission.code,
        permission.action,
        permission.description,
      ].some((field) => field?.toLowerCase().includes(value)),
    );
  }, [permissions, search]);

  return (
    <section className="page">
      <PageHeader
        eyebrow="Authorization"
        title="Permissions"
        description="Permission catalog reported by the admin API."
      />

      <FilterBar>
        <label>
          Search
          <input
            value={search}
            placeholder="permission code, action, description"
            onChange={(event) => setSearch(event.target.value)}
          />
        </label>
      </FilterBar>

      {loading && <LoadingState label="Loading permissions" />}
      {error && <ErrorState message={error} />}

      <DataTable
        rows={filteredPermissions as Record<string, unknown>[]}
        fallback="No permissions returned from the admin API."
        columns={[
          { key: "code", label: "Permission code" },
          { key: "action", label: "Action" },
          { key: "description", label: "Description" },
          {
            key: "active",
            label: "Status",
            render: (row) => (
              <StatusBadge
                status={row.active === false ? "Inactive" : "Active"}
              />
            ),
          },
        ]}
      />
    </section>
  );
}
