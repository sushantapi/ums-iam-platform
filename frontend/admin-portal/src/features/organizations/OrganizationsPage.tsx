import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { DataTable } from "../../components/ui/DataTable";
import { ErrorState } from "../../components/ui/ErrorState";
import { FilterBar } from "../../components/ui/FilterBar";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { Pagination } from "../../components/ui/Pagination";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { adminApi, type OrganizationResponse, type PageResponse } from "../../lib/api";

const pageSize = 20;

export function OrganizationsPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [organizations, setOrganizations] =
    useState<PageResponse<OrganizationResponse>>({
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
    setError(undefined);

    adminApi
      .organizations({ page, size: pageSize, search })
      .then(setOrganizations)
      .catch((err: Error) =>
        setError(`Organizations could not be loaded: ${err.message}`),
      )
      .finally(() => setLoading(false));
  }, [page, search]);

  return (
    <section className="page">
      <PageHeader
        eyebrow="Tenancy"
        title="Organizations"
        description="Organization catalog reported by the admin API."
      />

      <FilterBar>
        <label>
          Search
          <input
            value={search}
            placeholder="Name or slug"
            onChange={(event) => {
              setPage(0);
              setSearch(event.target.value);
            }}
          />
        </label>
      </FilterBar>

      {loading && <LoadingState label="Loading organizations" />}
      {error && <ErrorState message={error} />}

      <DataTable
        rows={organizations.content as Record<string, unknown>[]}
        fallback="No organizations returned from the admin API."
        onRowClick={(row) =>
          navigate(`/organizations/${String(row.id)}`)
        }
        columns={[
          { key: "name", label: "Name" },
          { key: "slug", label: "Slug" },
          { key: "description", label: "Description" },
          { key: "ownerId", label: "Owner ID" },
          {
            key: "status",
            label: "Status",
            render: (row) => (
              <StatusBadge status={String(row.status ?? "Unknown")} />
            ),
          },
        ]}
      />

      <Pagination
        page={page}
        size={pageSize}
        totalElements={organizations.totalElements}
        onPageChange={setPage}
      />
    </section>
  );
}
