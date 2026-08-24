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

function optionalValue(value: string): string | undefined {
  const normalized = value.trim();
  return normalized ? normalized : undefined;
}

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
  const [displayName, setDisplayName] = useState("");
  const [dateOfJoining, setDateOfJoining] = useState("");
  const [departmentId, setDepartmentId] = useState("");
  const [designationId, setDesignationId] = useState("");
  const [status, setStatus] = useState<EmployeeStatus>("ACTIVE");
  const [panNumber, setPanNumber] = useState("");
  const [uanNumber, setUanNumber] = useState("");
  const [esiNumber, setEsiNumber] = useState("");
  const [bankAccountNumber, setBankAccountNumber] = useState("");
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
        setDisplayName(employeeResponse.displayName ?? "");
        setDateOfJoining(employeeResponse.dateOfJoining ?? "");
        setDepartmentId(employeeResponse.departmentId ?? "");
        setDesignationId(employeeResponse.designationId ?? "");
        setStatus(employeeResponse.status);
        setPanNumber("");
        setUanNumber("");
        setEsiNumber("");
        setBankAccountNumber("");
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
        displayName: displayName.trim() || null,
        dateOfJoining: dateOfJoining || undefined,
        panNumber: optionalValue(panNumber),
        uanNumber: optionalValue(uanNumber),
        esiNumber: optionalValue(esiNumber),
        bankAccountNumber: optionalValue(bankAccountNumber),
      });
      setEmployee(updated);
      setEmployeeCode(updated.employeeCode);
      setDisplayName(updated.displayName ?? "");
      setDateOfJoining(updated.dateOfJoining ?? "");
      setPanNumber("");
      setUanNumber("");
      setEsiNumber("");
      setBankAccountNumber("");
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
      title={employee.displayName || employee.employeeCode}
      description="Reusable employee identity and masked payslip presentation details for this tenant."
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
            <li><strong>Employee code:</strong> {employee.employeeCode}</li>
            <li><strong>Employee name:</strong> {employee.displayName ?? "-"}</li>
            <li><strong>Date of joining:</strong> {employee.dateOfJoining ?? "-"}</li>
            <li><strong>UMS user ID:</strong> {employee.umsUserId}</li>
            <li><strong>Organization ID:</strong> {employee.organizationId}</li>
            <li><strong>Department:</strong> {departmentName}</li>
            <li><strong>Designation:</strong> {designationName}</li>
            <li><strong>PAN:</strong> {employee.panDisplay ?? "-"}</li>
            <li><strong>UAN:</strong> {employee.uanDisplay ?? "-"}</li>
            <li><strong>ESI:</strong> {employee.esiDisplay ?? "-"}</li>
            <li><strong>Bank account:</strong> {employee.bankAccountDisplay ?? "-"}</li>
            <li><strong>Status:</strong> <StatusBadge status={employee.status} /></li>
          </ul>
        </section>
      }
    >
      {error ? <ErrorState message={error} /> : null}
      {editing && canUpdate ? (
        <section className="panel">
          <h2>Update employee</h2>
          <p>
            Existing PAN, UAN, ESI and bank values are never returned in raw form. Leave a replacement field blank to keep its current masked value.
          </p>
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
              Employee display name
              <input
                maxLength={255}
                value={displayName}
                onChange={(event) => setDisplayName(event.target.value)}
                disabled={saving}
              />
            </label>
            <label>
              Date of joining
              <input
                type="date"
                value={dateOfJoining}
                onChange={(event) => setDateOfJoining(event.target.value)}
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
              Replace PAN number
              <input
                maxLength={64}
                value={panNumber}
                onChange={(event) => setPanNumber(event.target.value)}
                disabled={saving}
                autoComplete="off"
                placeholder={employee.panDisplay ?? "Not set"}
              />
            </label>
            <label>
              Replace UAN number
              <input
                maxLength={64}
                value={uanNumber}
                onChange={(event) => setUanNumber(event.target.value)}
                disabled={saving}
                autoComplete="off"
                placeholder={employee.uanDisplay ?? "Not set"}
              />
            </label>
            <label>
              Replace ESI number
              <input
                maxLength={64}
                value={esiNumber}
                onChange={(event) => setEsiNumber(event.target.value)}
                disabled={saving}
                autoComplete="off"
                placeholder={employee.esiDisplay ?? "Not set"}
              />
            </label>
            <label>
              Replace bank account number
              <input
                maxLength={64}
                value={bankAccountNumber}
                onChange={(event) => setBankAccountNumber(event.target.value)}
                disabled={saving}
                autoComplete="off"
                placeholder={employee.bankAccountDisplay ?? "Not set"}
              />
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
