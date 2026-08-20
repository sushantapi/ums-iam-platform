import { FormEvent, useEffect, useState } from "react";
import { DataTable } from "../../components/ui/DataTable";
import { ErrorState } from "../../components/ui/ErrorState";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { StatusBadge } from "../../components/ui/StatusBadge";
import {
  hrmsApi,
  type DepartmentResponse,
  type DesignationResponse,
  type MasterDataStatus,
} from "../../lib/api";
import { hasAdminCapability } from "../../lib/auth/capabilities";
import { HrmsOrganizationScope } from "./HrmsOrganizationScope";
import { getStoredHrmsOrganizationId } from "./hrmsOrganizationScopeStorage";

type Kind = "departments" | "designations";
type MasterRecord = DepartmentResponse | DesignationResponse;

export function HrmsMasterDataPage({ kind }: { kind: Kind }) {
  const title = kind === "departments" ? "Departments" : "Designations";
  const singular = kind === "departments" ? "department" : "designation";
  const canCreate = hasAdminCapability(
    kind === "departments"
      ? "hrms.departments.create"
      : "hrms.designations.create",
  );
  const canUpdate = hasAdminCapability(
    kind === "departments"
      ? "hrms.departments.update"
      : "hrms.designations.update",
  );

  const [organizationId, setOrganizationId] = useState(
    getStoredHrmsOrganizationId(),
  );
  const [records, setRecords] = useState<MasterRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>();
  const [editing, setEditing] = useState<MasterRecord>();
  const [code, setCode] = useState("");
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [status, setStatus] = useState<MasterDataStatus>("ACTIVE");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!organizationId) {
      setRecords([]);
      return;
    }

    setLoading(true);
    setError(undefined);
    const request =
      kind === "departments"
        ? hrmsApi.departments({ organizationId, page: 0, size: 100 })
        : hrmsApi.designations({ organizationId, page: 0, size: 100 });

    request
      .then((response) => setRecords(response.content))
      .catch((err: Error) => setError(`${title} could not be loaded: ${err.message}`))
      .finally(() => setLoading(false));
  }, [kind, organizationId, title]);

  function resetForm() {
    setEditing(undefined);
    setCode("");
    setName("");
    setDescription("");
    setStatus("ACTIVE");
  }

  function beginEdit(record: MasterRecord) {
    if (!canUpdate) {
      return;
    }
    setEditing(record);
    setCode(record.code);
    setName(record.name);
    setDescription(record.description ?? "");
    setStatus(record.status);
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!organizationId) {
      setError("Select an organization before managing HRMS master data.");
      return;
    }

    setSaving(true);
    setError(undefined);
    try {
      if (kind === "departments") {
        if (editing) {
          await hrmsApi.updateDepartment(editing.id, {
            organizationId,
            code: code.trim(),
            name: name.trim(),
            description: description.trim() || null,
            status,
          });
        } else {
          await hrmsApi.createDepartment({
            organizationId,
            code: code.trim(),
            name: name.trim(),
            description: description.trim() || null,
          });
        }
      } else if (editing) {
        await hrmsApi.updateDesignation(editing.id, {
          organizationId,
          code: code.trim(),
          name: name.trim(),
          description: description.trim() || null,
          status,
        });
      } else {
        await hrmsApi.createDesignation({
          organizationId,
          code: code.trim(),
          name: name.trim(),
          description: description.trim() || null,
        });
      }

      resetForm();
      const refreshed =
        kind === "departments"
          ? await hrmsApi.departments({ organizationId, page: 0, size: 100 })
          : await hrmsApi.designations({ organizationId, page: 0, size: 100 });
      setRecords(refreshed.content);
    } catch (err) {
      setError(`${title} could not be saved: ${(err as Error).message}`);
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="page">
      <PageHeader
        eyebrow="HRMS"
        title={title}
        description={`Tenant-scoped ${title.toLowerCase()} used by employee records.`}
      />

      <HrmsOrganizationScope
        organizationId={organizationId}
        onChange={setOrganizationId}
      />

      {(canCreate || editing) && organizationId ? (
        <section className="panel">
          <h2>{editing ? `Update ${singular}` : `Create ${singular}`}</h2>
          <form onSubmit={save}>
            <label>
              Code
              <input
                required
                maxLength={64}
                value={code}
                onChange={(event) => setCode(event.target.value)}
                disabled={saving}
              />
            </label>
            <label>
              Name
              <input
                required
                maxLength={120}
                value={name}
                onChange={(event) => setName(event.target.value)}
                disabled={saving}
              />
            </label>
            <label>
              Description
              <textarea
                maxLength={500}
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                disabled={saving}
              />
            </label>
            {editing ? (
              <label>
                Status
                <select
                  value={status}
                  onChange={(event) =>
                    setStatus(event.target.value as MasterDataStatus)
                  }
                  disabled={saving}
                >
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="INACTIVE">INACTIVE</option>
                </select>
              </label>
            ) : null}
            <div>
              <button type="submit" className="button-primary" disabled={saving}>
                {saving ? "Saving..." : editing ? "Update" : "Create"}
              </button>{" "}
              {editing ? (
                <button type="button" onClick={resetForm} disabled={saving}>
                  Cancel
                </button>
              ) : null}
            </div>
          </form>
        </section>
      ) : null}

      {!organizationId ? (
        <ErrorState message="Enter the tenant organization ID to load HRMS master data." />
      ) : null}
      {loading ? <LoadingState label={`Loading ${title.toLowerCase()}`} /> : null}
      {error ? <ErrorState message={error} /> : null}

      {organizationId && !loading ? (
        <DataTable
          rows={records as Array<Record<string, unknown>>}
          fallback={`No ${title.toLowerCase()} found for this organization.`}
          onRowClick={canUpdate ? (row) => beginEdit(row as MasterRecord) : undefined}
          columns={[
            { key: "code", label: "Code" },
            { key: "name", label: "Name" },
            { key: "description", label: "Description" },
            {
              key: "status",
              label: "Status",
              render: (row) => (
                <StatusBadge status={String(row.status ?? "Unknown")} />
              ),
            },
          ]}
        />
      ) : null}
    </section>
  );
}
