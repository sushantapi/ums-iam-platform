import { FormEvent, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { DetailLayout } from "../../components/layout/DetailLayout";
import { ErrorState } from "../../components/ui/ErrorState";
import { LoadingState } from "../../components/ui/LoadingState";
import { StatusBadge } from "../../components/ui/StatusBadge";
import {
  hrmsApi,
  type DepartmentResponse,
  type DesignationResponse,
  type EmployeeResponse,
  type EmployeeStatus,
} from "../../lib/api";
import { hasAdminCapability } from "../../lib/auth/capabilities";
import { getStoredHrmsOrganizationId } from "./hrmsOrganizationScopeStorage";

export function EmployeeDetailPage() {
  const { employeeId = "" } = useParams();
  const navigate = useNavigate();
  const organizationId = getStoredHrmsOrganizationId();
  const canUpdate = hasAdminCapability("hrms.employees.update");
  const [employee, setEmployee] = useState<EmployeeResponse>();
  const [departments, setDepartments] = useState<DepartmentResponse[]>([]);
  const [designations, setDesignations] = useState<DesignationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();
  const [editing, setEditing] = useState(false);
  const [employeeCode, setEmployeeCode] = useState("");
  const [departmentId, setDepartmentId] = useState("");
  const [designationId, setDesignationId] = useState("");
  const [status, setStatus] = useState<EmployeeStatus>("ACTIVE");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!employeeId || !organizationId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(undefined);
    Promise.all([
      hrmsApi.employeeDetail(employeeId, organizationId),
      hrmsApi.departments({ organizationId, page: 0, size: 100 }),
      hrmsApi.designations({ organizationId, page: 0, size: 100 }),
    ])
      .then(([employeeResponse, departmentPage, designationPage]) => {
        setEmployee(employeeResponse);
        setDepartments(departmentPage.content);
        setDesignations(designationPage.content);
        setEmployeeCode(employeeResponse.employeeCode);
        setDepartmentId(employeeResponse.departmentId ?? "");
        setDesignationId(employeeResponse.designationId ?? "");
        setStatus(employeeResponse.status);
      })
      .catch((err: Error) =>
        setError(`Employee could not be loaded: ${err.message}`),
      )
      .finally(() => setLoading(false));
  }, [employeeId, organizationId]);

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!employee || !organizationId) {
      return;
    }

    setSaving(true);
    setError(undefined);
    try {
      const updated = await hrmsApi.updateEmployee(employee.id, {
        organizationId,
        employeeCode: employeeCode.trim(),
        departmentId: departmentId || null,
        designationId: designationId || null,
        status,
      });
      setEmployee(updated);
      setEditing(false);
    } catch (err) {
      setError(`Employee could not be updated: ${(err as Error).message}`);
    } finally {
      setSaving(false);
    }
  }

  if (!organizationId) {
    return (
      <section className="page">
        <ErrorState message="Select an organization from the Employees screen before opening employee details." />
      </section>
    );
  }

  if (loading) {
    return (
      <section className="page">
        <LoadingState label="Loading employee" />
      </section>
    );
  }

  if (error || !employee) {
    return (
      <section className="page">
        <ErrorState message={error ?? "Employee was not found."} />
      </section>
    );
  }

  const departmentName =
    departments.find((item) => item.id === employee.departmentId)?.name ?? "-";
  const designationName =
    designations.find((item) => item.id === employee.designationId)?.name ?? "-";

  return (
    <DetailLayout
      eyebrow="HRMS Employee"
      title={employee.employeeCode}
      description="Employee identity, tenant references, and lifecycle status."
      actions={
        <>
          <button type="button" onClick={() => navigate("/hrms/employees")}>
            Back
          </button>{" "}
          {canUpdate ? (
            <button
              type="button"
              className="button-primary"
              onClick={() => setEditing((current) => !current)}
            >
              {editing ? "Cancel edit" : "Edit employee"}
            </button>
          ) : null}
        </>
      }
      summary={
        <section className="panel">
          <ul className="detail-list">
            <li><strong>UMS user ID:</strong> {employee.umsUserId}</li>
            <li><strong>Organization ID:</strong> {employee.organizationId}</li>
            <li><strong>Department:</strong> {departmentName}</li>
            <li><strong>Designation:</strong> {designationName}</li>
            <li><strong>Status:</strong> <StatusBadge status={employee.status} /></li>
          </ul>
        </section>
      }
    >
      {error ? <ErrorState message={error} /> : null}
      {editing && canUpdate ? (
        <section className="panel">
          <h2>Update employee</h2>
          <form onSubmit={save}>
            <label>
              Employee code
              <input
                required
                maxLength={64}
                value={employeeCode}
                onChange={(event) => setEmployeeCode(event.target.value)}
                disabled={saving}
              />
            </label>
            <label>
              Department
              <select
                value={departmentId}
                onChange={(event) => setDepartmentId(event.target.value)}
                disabled={saving}
              >
                <option value="">No department</option>
                {departments.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.code} - {item.name} ({item.status})
                  </option>
                ))}
              </select>
            </label>
            <label>
              Designation
              <select
                value={designationId}
                onChange={(event) => setDesignationId(event.target.value)}
                disabled={saving}
              >
                <option value="">No designation</option>
                {designations.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.code} - {item.name} ({item.status})
                  </option>
                ))}
              </select>
            </label>
            <label>
              Status
              <select
                value={status}
                onChange={(event) => setStatus(event.target.value as EmployeeStatus)}
                disabled={saving}
              >
                <option value="ACTIVE">ACTIVE</option>
                <option value="INACTIVE">INACTIVE</option>
                <option value="TERMINATED">TERMINATED</option>
              </select>
            </label>
            <button type="submit" className="button-primary" disabled={saving}>
              {saving ? "Saving..." : "Save changes"}
            </button>
          </form>
        </section>
      ) : null}
    </DetailLayout>
  );
}
