import { FormEvent, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import organizationAdminService from "../../api/services/organizationAdminService";
import { DataTable } from "../../components/ui/DataTable";
import { ErrorState } from "../../components/ui/ErrorState";
import { FilterBar } from "../../components/ui/FilterBar";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { Pagination } from "../../components/ui/Pagination";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { adminApi, type OrganizationResponse, type PageResponse } from "../../lib/api";
import { hasAdminCapability } from "../../lib/auth/capabilities";

const pageSize = 20;

export function OrganizationsPage() {
  const navigate = useNavigate();
  const canManageOrganizations = hasAdminCapability("organizations.manage");
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
  const [showCreate, setShowCreate] = useState(false);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [creating, setCreating] = useState(false);

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

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const normalizedName = name.trim();
    if (!normalizedName) {
      setError("Organization name is required.");
      return;
    }

    setCreating(true);
    setError(undefined);

    try {
      const created = await organizationAdminService.create({
        name: normalizedName,
        description: description.trim() || undefined,
      });

      setName("");
      setDescription("");
      setShowCreate(false);
      navigate(`/organizations/${created.id}`);
    } catch (err) {
      setError(`Organization could not be created: ${(err as Error).message}`);
    } finally {
      setCreating(false);
    }
  }

  return (
    <section className="page">
      <PageHeader
        eyebrow="Tenancy"
        title="Organizations"
        description="Create, search, and inspect organizations through the UMS admin API."
        actions={
          canManageOrganizations ? (
            <button
              type="button"
              className="button-primary"
              onClick={() => setShowCreate((current) => !current)}
            >
              {showCreate ? "Cancel" : "Create organization"}
            </button>
          ) : undefined
        }
      />

      {showCreate && canManageOrganizations ? (
        <section className="panel">
          <h2>Create organization</h2>
          <form onSubmit={handleCreate}>
            <label>
              Name
              <input
                value={name}
                maxLength={255}
                onChange={(event) => setName(event.target.value)}
                disabled={creating}
                required
              />
            </label>

            <label>
              Description
              <textarea
                value={description}
                maxLength={500}
                onChange={(event) => setDescription(event.target.value)}
                disabled={creating}
              />
            </label>

            <div>
              <button
                type="submit"
                className="button-primary"
                disabled={creating}
              >
                {creating ? "Creating..." : "Create"}
              </button>
            </div>
          </form>
        </section>
      ) : null}

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
