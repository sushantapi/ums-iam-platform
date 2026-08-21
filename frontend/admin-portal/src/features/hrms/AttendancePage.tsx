import { FormEvent, useEffect, useMemo, useState } from "react";
import { DataTable } from "../../components/ui/DataTable";
import { ErrorState } from "../../components/ui/ErrorState";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { Pagination } from "../../components/ui/Pagination";
import { StatusBadge } from "../../components/ui/StatusBadge";
import {
  hrmsApi,
  type EmployeeResponse,
  type PageResponse,
} from "../../lib/api";
import { hasAdminCapability } from "../../lib/auth/capabilities";
import {
  attendanceApi,
  type AttendanceResponse,
  type AttendanceStatus,
} from "./attendanceApi";
import { HrmsOrganizationScope } from "./HrmsOrganizationScope";
import { getStoredHrmsOrganizationId } from "./hrmsOrganizationScopeStorage";

const pageSize = 20;

function toApiDateTime(value: string): string | null {
  if (!value) {
    return null;
  }
  return value.length === 16 ? `${value}:00` : value;
}

function toInputDateTime(value: string | null): string {
  return value ? value.slice(0, 16) : "";
}

export function AttendancePage() {
  const canCreate = hasAdminCapability("hrms.attendance.create");
  const canUpdate = hasAdminCapability("hrms.attendance.update");
  const [organizationId, setOrganizationId] = useState(
    getStoredHrmsOrganizationId(),
  );
  const [page, setPage] = useState(0);
  const [attendance, setAttendance] = useState<PageResponse<AttendanceResponse>>({
    content: [],
    page: 0,
    size: pageSize,
    totalElements: 0,
    totalPages: 0,
  });
  const [employees, setEmployees] = useState<EmployeeResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>();
  const [showCreate, setShowCreate] = useState(false);
  const [editing, setEditing] = useState<AttendanceResponse>();
  const [employeeId, setEmployeeId] = useState("");
  const [workDate, setWorkDate] = useState("");
  const [status, setStatus] = useState<AttendanceStatus>("PRESENT");
  const [checkInAt, setCheckInAt] = useState("");
  const [checkOutAt, setCheckOutAt] = useState("");
  const [notes, setNotes] = useState("");
  const [saving, setSaving] = useState(false);

  async function load(currentPage = page) {
    if (!organizationId) {
      setAttendance({
        content: [],
        page: 0,
        size: pageSize,
        totalElements: 0,
        totalPages: 0,
      });
      setEmployees([]);
      return;
    }

    setLoading(true);
    setError(undefined);
    try {
      const [attendancePage, employeePage] = await Promise.all([
        attendanceApi.list({
          organizationId,
          page: currentPage,
          size: pageSize,
        }),
        hrmsApi.employees({ organizationId, page: 0, size: 200 }),
      ]);
      setAttendance(attendancePage);
      setEmployees(employeePage.content);
    } catch (err) {
      setError(`Attendance could not be loaded: ${(err as Error).message}`);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
    // load is intentionally scoped to organization/page changes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [organizationId, page]);

  const employeeLabels = useMemo(
    () => new Map(employees.map((employee) => [employee.id, employee.employeeCode])),
    [employees],
  );

  function resetForm() {
    setEditing(undefined);
    setEmployeeId("");
    setWorkDate("");
    setStatus("PRESENT");
    setCheckInAt("");
    setCheckOutAt("");
    setNotes("");
  }

  function beginEdit(record: AttendanceResponse) {
    if (!canUpdate) {
      return;
    }
    setShowCreate(false);
    setEditing(record);
    setEmployeeId(record.employeeId);
    setWorkDate(record.workDate);
    setStatus(record.status);
    setCheckInAt(toInputDateTime(record.checkInAt));
    setCheckOutAt(toInputDateTime(record.checkOutAt));
    setNotes(record.notes ?? "");
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!organizationId) {
      setError("Select an organization before managing attendance.");
      return;
    }

    setSaving(true);
    setError(undefined);
    try {
      if (editing) {
        await attendanceApi.update(editing.id, {
          organizationId,
          status,
          checkInAt: toApiDateTime(checkInAt),
          checkOutAt: toApiDateTime(checkOutAt),
          notes: notes.trim() || null,
        });
      } else {
        await attendanceApi.create({
          organizationId,
          employeeId,
          workDate,
          status,
          checkInAt: toApiDateTime(checkInAt),
          checkOutAt: toApiDateTime(checkOutAt),
          notes: notes.trim() || null,
        });
      }

      resetForm();
      setShowCreate(false);
      if (page !== 0 && !editing) {
        setPage(0);
      } else {
        await load(page !== 0 && !editing ? 0 : page);
      }
    } catch (err) {
      setError(`Attendance could not be saved: ${(err as Error).message}`);
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="page">
      <PageHeader
        eyebrow="HRMS"
        title="Attendance"
        description="Tenant-scoped daily attendance using validated employee references through the API Gateway."
        actions={
          canCreate && organizationId ? (
            <button
              type="button"
              className="button-primary"
              onClick={() => {
                resetForm();
                setShowCreate((current) => !current);
              }}
            >
              {showCreate ? "Cancel" : "Create attendance"}
            </button>
          ) : undefined
        }
      />

      <HrmsOrganizationScope
        organizationId={organizationId}
        onChange={(value) => {
          setPage(0);
          resetForm();
          setShowCreate(false);
          setOrganizationId(value);
        }}
      />

      {(showCreate || editing) && organizationId ? (
        <section className="panel">
          <h2>{editing ? "Update attendance" : "Create attendance"}</h2>
          <form onSubmit={save}>
            <label>
              Employee
              <select
                required
                value={employeeId}
                onChange={(event) => setEmployeeId(event.target.value)}
                disabled={saving || Boolean(editing)}
              >
                <option value="">Select employee</option>
                {employees
                  .filter((employee) => employee.status === "ACTIVE")
                  .map((employee) => (
                    <option key={employee.id} value={employee.id}>
                      {employee.employeeCode}
                    </option>
                  ))}
              </select>
            </label>
            <label>
              Work date
              <input
                required
                type="date"
                value={workDate}
                onChange={(event) => setWorkDate(event.target.value)}
                disabled={saving || Boolean(editing)}
              />
            </label>
            <label>
              Status
              <select
                value={status}
                onChange={(event) =>
                  setStatus(event.target.value as AttendanceStatus)
                }
                disabled={saving}
              >
                <option value="PRESENT">PRESENT</option>
                <option value="ABSENT">ABSENT</option>
                <option value="HALF_DAY">HALF_DAY</option>
                <option value="HOLIDAY">HOLIDAY</option>
              </select>
            </label>
            <label>
              Check in
              <input
                type="datetime-local"
                value={checkInAt}
                onChange={(event) => setCheckInAt(event.target.value)}
                disabled={saving}
              />
            </label>
            <label>
              Check out
              <input
                type="datetime-local"
                value={checkOutAt}
                onChange={(event) => setCheckOutAt(event.target.value)}
                disabled={saving}
              />
            </label>
            <label>
              Notes
              <textarea
                maxLength={500}
                value={notes}
                onChange={(event) => setNotes(event.target.value)}
                disabled={saving}
              />
            </label>
            <div>
              <button type="submit" className="button-primary" disabled={saving}>
                {saving ? "Saving..." : editing ? "Update" : "Create"}
              </button>{" "}
              {editing ? (
                <button
                  type="button"
                  onClick={() => {
                    resetForm();
                    setShowCreate(false);
                  }}
                  disabled={saving}
                >
                  Cancel
                </button>
              ) : null}
            </div>
          </form>
        </section>
      ) : null}

      {!organizationId ? (
        <ErrorState message="Enter the tenant organization ID to load attendance." />
      ) : null}
      {loading ? <LoadingState label="Loading attendance" /> : null}
      {error ? <ErrorState message={error} /> : null}

      {organizationId && !loading ? (
        <>
          <DataTable
            rows={attendance.content as Array<Record<string, unknown>>}
            fallback="No attendance records found for this organization."
            onRowClick={
              canUpdate
                ? (row) => beginEdit(row as unknown as AttendanceResponse)
                : undefined
            }
            columns={[
              { key: "workDate", label: "Date" },
              {
                key: "employeeId",
                label: "Employee",
                render: (row) =>
                  employeeLabels.get(String(row.employeeId ?? "")) ??
                  String(row.employeeId ?? "-"),
              },
              {
                key: "status",
                label: "Status",
                render: (row) => (
                  <StatusBadge status={String(row.status ?? "Unknown")} />
                ),
              },
              { key: "checkInAt", label: "Check in" },
              { key: "checkOutAt", label: "Check out" },
              { key: "notes", label: "Notes" },
            ]}
          />
          <Pagination
            page={page}
            size={pageSize}
            totalElements={attendance.totalElements}
            onPageChange={setPage}
          />
        </>
      ) : null}
    </section>
  );
}
