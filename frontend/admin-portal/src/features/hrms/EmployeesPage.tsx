import { FormEvent, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { DataTable } from "../../components/ui/DataTable";
import { ErrorState } from "../../components/ui/ErrorState";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { Pagination } from "../../components/ui/Pagination";
import { StatusBadge } from "../../components/ui/StatusBadge";
import {
  hrmsApi,
  type DepartmentResponse,
  type DesignationResponse,
  type EmployeeResponse,
  type PageResponse,
} from "../../lib/api";
import { hasAdminCapability } from "../../lib/auth/capabilities";
import { HrmsOrganizationScope } from "./HrmsOrganizationScope";
import { getStoredHrmsOrganizationId } from "./hrmsOrganizationScopeStorage";

const pageSize = 20;

function nullable(value: string): string | null {
  const normalized = value.trim();
  return normalized ? normalized : null;
}

export function EmployeesPage() {
  const navigate = useNavigate();
  const canCreate = hasAdminCapability("hrms.employees.create");
  const [organizationId, setOrganizationId] = useState(
    getStoredHrmsOrganizationId(),
  );
  const [page, setPage] = useState(0);
  const [employees, setEmployees] = useState<PageResponse<EmployeeResponse>>({
    content: [],
    page: 0,
    size: pageSize,
    totalElements: 0,
    totalPages: 0,
  });
  const [departments, setDepartments] = useState<DepartmentResponse[]>([]);
  const [designations, setDesignations] = useState<DesignationResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>();
  const [showCreate, setShowCreate] = useState(false);
  const [umsUserId, setUmsUserId] = useState("");
  const [employeeCode, setEmployeeCode] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [dateOfJoining, setDateOfJoining] = useState("");
  const [departmentId, setDepartmentId] = useState("");
  const [designationId, setDesignationId] = useState("");
  const [panNumber, setPanNumber] = useState("");
  const [uanNumber, setUanNumber] = useState("");
  const [esiNumber, setEsiNumber] = useState("");
  const [bankAccountNumber, setBankAccountNumber] = useState("");
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    if (!organizationId) {
      setEmployees({
        content: [],
        page: 0,
        size: pageSize,
        totalElements: 0,
        totalPages: 0,
      });
      setDepartments([]);
      setDesignations([]);
      return;
    }

    setLoading(true);
    setError(undefined);

    Promise.all([
      hrmsApi.employees({ organizationId, page, size: pageSize }),
      hrmsApi.departments({ organizationId, page: 0, size: 100 }),
      hrmsApi.designations({ organizationId, page: 0, size: 100 }),
    ])
      .then(([employeePage, departmentPage, designationPage]) => {
        setEmployees(employeePage);
        setDepartments(departmentPage.content);
        setDesignations(designationPage.content);
      })
      .catch((err: Error) =>
        setError(`Employees could not be loaded: ${err.message}`),
      )
      .finally(() => setLoading(false));
  }, [organizationId, page]);

  const departmentNames = useMemo(
    () => new Map(departments.map((item) => [item.id, item.name])),
    [departments],
  );
  const designationNames = useMemo(
    () => new Map(designations.map((item) => [item.id, item.name])),
    [designations],
  );

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!organizationId) {
      setError("Select an organization before creating an employee.");
      return;
    }

    setCreating(true);
    setError(undefined);
    try {
      const created = await hrmsApi.createEmployee({
        organizationId,
        umsUserId: umsUserId.trim(),
        employeeCode: employeeCode.trim(),
        departmentId: departmentId || null,
        designationId: designationId || null,
        displayName: nullable(displayName),
        dateOfJoining: dateOfJoining || null,
        panNumber: nullable(panNumber),
        uanNumber: nullable(uanNumber),
        esiNumber: nullable(esiNumber),
        bankAccountNumber: nullable(bankAccountNumber),
      });
      setUmsUserId("");
      setEmployeeCode("");
      setDisplayName("");
      setDateOfJoining("");
      setDepartmentId("");
      setDesignationId("");
      setPanNumber("");
      setUanNumber("");
      setEsiNumber("");
      setBankAccountNumber("");
      setShowCreate(false);
      navigate(`/hrms/employees/${created.id}`);
    } catch (err) {
      setError(`Employee could not be created: ${(err as Error).message}`);
    } finally {
      setCreating(false);
    }
  }

  return (
    <section className="page">
      <PageHeader
        eyebrow="HRMS"
        title="Employees"
        description="Maintain tenant employee identity and reusable payslip details once, then snapshot them into payroll."
        actions={
          canCreate && organizationId ? (
            <button
              type="button"
              className="button-primary"
              onClick={() => setShowCreate((current) => !current)}
            >
              {showCreate ? "Cancel" : "Create employee"}
            </button>
          ) : undefined
        }
      />

      <HrmsOrganizationScope
        organizationId={organizationId}
        onChange={(value) => {
          setPage(0);
          setOrganizationId(value);
        }}
      />

      {showCreate && canCreate && organizationId ? (
        <section className="panel">
          <h2>Create employee</h2>
          <p>
            Payslip identifiers are accepted once and stored only as masked display values.
          </p>
          <form onSubmit={handleCreate}>
            <label>
              UMS user ID
              <input
                required
                value={umsUserId}
                placeholder="Existing IAM user UUID"
                onChange={(event) => setUmsUserId(event.target.value)}
                disabled={creating}
              />
            </label>
            <label>
              Employee code
              <input
                required
                maxLength={64}
                value={employeeCode}
                onChange={(event) => setEmployeeCode(event.target.value)}
                disabled={creating}
              />
            </label>
            <label>
              Employee display name
              <input
                maxLength={255}
                value={displayName}
                onChange={(event) => setDisplayName(event.target.value)}
                disabled={creating}
              />
            </label>
            <label>
              Date of joining
              <input
                type="date"
                value={dateOfJoining}
                onChange={(event) => setDateOfJoining(event.target.value)}
                disabled={creating}
              />
            </label>
            <label>
              Department
              <select
                value={departmentId}
                onChange={(event) => setDepartmentId(event.target.value)}
                disabled={creating}
              >
                <option value="">No department</option>
                {departments
                  .filter((item) => item.status === "ACTIVE")
                  .map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.code} - {item.name}
                    </option>
                  ))}
              </select>
            </label>
            <label>
              Designation
              <select
                value={designationId}
                onChange={(event) => setDesignationId(event.target.value)}
                disabled={creating}
              >
                <option value="">No designation</option>
                {designations
                  .filter((item) => item.status === "ACTIVE")
                  .map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.code} - {item.name}
                    </option>
                  ))}
              </select>
            </label>
            <label>
              PAN number
              <input
                maxLength={64}
                value={panNumber}
                onChange={(event) => setPanNumber(event.target.value)}
                disabled={creating}
                autoComplete="off"
              />
            </label>
            <label>
              UAN number
              <input
                maxLength={64}
                value={uanNumber}
                onChange={(event) => setUanNumber(event.target.value)}
                disabled={creating}
                autoComplete="off"
              />
            </label>
            <label>
              ESI number
              <input
                maxLength={64}
                value={esiNumber}
                onChange={(event) => setEsiNumber(event.target.value)}
                disabled={creating}
                autoComplete="off"
              />
            </label>
            <label>
              Bank account number
              <input
                maxLength={64}
                value={bankAccountNumber}
                onChange={(event) => setBankAccountNumber(event.target.value)}
                disabled={creating}
                autoComplete="off"
              />
            </label>
            <button type="submit" className="button-primary" disabled={creating}>
              {creating ? "Creating..." : "Create employee"}
            </button>
          </form>
        </section>
      ) : null}

      {!organizationId ? (
        <ErrorState message="Enter the tenant organization ID to load employees." />
      ) : null}
      {loading ? <LoadingState label="Loading employees" /> : null}
      {error ? <ErrorState message={error} /> : null}

      {organizationId && !loading ? (
        <>
          <DataTable
            rows={employees.content as Array<Record<string, unknown>>}
            fallback="No employees found for this organization."
            onRowClick={(row) =>
              navigate(`/hrms/employees/${String(row.id)}`)
            }
            columns={[
              {
                key: "displayName",
                label: "Employee",
                render: (row) => String(row.displayName ?? "-")
              },
              { key: "employeeCode", label: "Employee code" },
              {
                key: "departmentId",
                label: "Department",
                render: (row) =>
                  departmentNames.get(String(row.departmentId ?? "")) ?? "-",
              },
              {
                key: "designationId",
                label: "Designation",
                render: (row) =>
                  designationNames.get(String(row.designationId ?? "")) ?? "-",
              },
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
            totalElements={employees.totalElements}
            onPageChange={setPage}
          />
        </>
      ) : null}
    </section>
  );
}
