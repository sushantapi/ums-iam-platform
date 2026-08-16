import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { DataTable } from "../../components/ui/DataTable";
import { ErrorState } from "../../components/ui/ErrorState";
import { FilterBar } from "../../components/ui/FilterBar";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { adminApi, type RoleResponse } from "../../lib/api";

export function RolesPage() {
  const navigate = useNavigate();

  const [roles, setRoles] = useState<RoleResponse[]>([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  useEffect(() => {
    setLoading(true);
    setError(undefined);

    adminApi
      .roles()
      .then(setRoles)
      .catch((err: Error) =>
        setError(`Roles could not be loaded: ${err.message}`),
      )
      .finally(() => setLoading(false));
  }, []);

  const visibleRoles = useMemo(() => {
    const query = search.trim().toLowerCase();

    if (!query) {
      return roles;
    }

    return roles.filter((role) =>
      [role.name, role.description]
        .filter(Boolean)
        .some((value) =>
          String(value).toLowerCase().includes(query),
        ),
    );
  }, [roles, search]);

  return (
    <section className="page">
      <PageHeader
        eyebrow="Authorization"
        title="Roles"
        description="Role catalog with backend-reported status and system/custom ownership."
      />

      <FilterBar>
        <label>
          Search
          <input
            value={search}
            placeholder="Role name or description"
            onChange={(event) => setSearch(event.target.value)}
          />
        </label>
      </FilterBar>

      {loading && <LoadingState label="Loading roles" />}
      {error && <ErrorState message={error} />}

      <DataTable
        rows={visibleRoles as Record<string, unknown>[]}
        fallback="No roles returned from the admin API."
        onRowClick={(row) =>
          navigate(`/roles/${String(row.id)}`)
        }
        columns={[
          {
            key: "name",
            label: "Role",
            render: (row) => String(row.name ?? "-"),
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
                status={row.system ? "System" : "Custom"}
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
  );
}
