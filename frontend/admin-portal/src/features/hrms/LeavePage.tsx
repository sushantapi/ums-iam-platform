import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
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
import { HrmsOrganizationScope } from "./HrmsOrganizationScope";
import { getStoredHrmsOrganizationId } from "./hrmsOrganizationScopeStorage";
import {
  leaveApi,
  type LeaveResponse,
  type LeaveType,
} from "./leaveApi";

const pageSize = 20;

const emptyEmployees: PageResponse<EmployeeResponse> = {
  content: [],
  page: 0,
  size: 100,
  totalElements: 0,
  totalPages: 0,
};

export function LeavePage() {
  const canReadEmployees = hasAdminCapability("hrms.employees.read");
  const canCreate = hasAdminCapability("hrms.leave.request");
  const canApprove = hasAdminCapability("hrms.leave.approve");
  const canCancel = hasAdminCapability("hrms.leave.cancel");

  const [organizationId, setOrganizationId] = useState(
    getStoredHrmsOrganizationId(),
  );
  const [page, setPage] = useState(0);
  const [leaves, setLeaves] = useState<PageResponse<LeaveResponse>>({
    content: [],
    page: 0,
    size: pageSize,
    totalElements: 0,
    totalPages: 0,
  });
  const [employees, setEmployees] = useState<EmployeeResponse[]>([]);
  const [selected, setSelected] = useState<LeaveResponse>();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>();

  const [showCreate, setShowCreate] = useState(false);
  const [employeeId, setEmployeeId] = useState("");
  const [leaveType, setLeaveType] = useState<LeaveType>("ANNUAL");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [reason, setReason] = useState("");
  const [creating, setCreating] = useState(false);

  const [decisionComment, setDecisionComment] = useState("");
  const [transitioning, setTransitioning] = useState(false);

  const loadData = useCallback(async () => {
    if (!organizationId) {
      setLeaves({
        content: [],
        page: 0,
        size: pageSize,
        totalElements: 0,
        totalPages: 0,
      });
      setEmployees([]);
      setSelected(undefined);
      return;
    }

    setLoading(true);
    setError(undefined);

    try {
      const [leavePage, employeePage] = await Promise.all([
        leaveApi.list({ organizationId, page, size: pageSize }),
        canReadEmployees
          ? hrmsApi.employees({ organizationId, page: 0, size: 100 })
          : Promise.resolve(emptyEmployees),
      ]);

      setLeaves(leavePage);
      setEmployees(employeePage.content);
      setSelected((current) =>
        current
          ? leavePage.content.find((item) => item.id === current.id) ?? current
          : undefined,
      );
    } catch (err) {
      setError(`Leave requests could not be loaded: ${(err as Error).message}`);
    } finally {
      setLoading(false);
    }
  }, [canReadEmployees, organizationId, page]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const employeeCodes = useMemo(
    () => new Map(employees.map((employee) => [employee.id, employee.employeeCode])),
    [employees],
  );

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!organizationId) {
      setError("Select an organization before creating a leave request.");
      return;
    }
    if (!employeeId.trim()) {
      setError("Employee is required.");
      return;
    }
    if (!startDate || !endDate) {
      setError("Start date and end date are required.");
      return;
    }

    setCreating(true);
    setError(undefined);

    try {
      const created = await leaveApi.create({
        organizationId,
        employeeId: employeeId.trim(),
        leaveType,
        startDate,
        endDate,
        reason: reason.trim() || null,
      });

      setEmployeeId("");
      setLeaveType("ANNUAL");
      setStartDate("");
      setEndDate("");
      setReason("");
      setShowCreate(false);
      setSelected(created);
      setDecisionComment("");

      if (page !== 0) {
        setPage(0);
      } else {
        await loadData();
      }
    } catch (err) {
      setError(`Leave request could not be created: ${(err as Error).message}`);
    } finally {
      setCreating(false);
    }
  }

  async function transition(action: "approve" | "reject" | "cancel") {
    if (!selected || !organizationId || selected.status !== "PENDING") {
      return;
    }

    setTransitioning(true);
    setError(undefined);

    try {
      const body = {
        organizationId,
        decisionComment: decisionComment.trim() || null,
      };
      const updated =
        action === "approve"
          ? await leaveApi.approve(selected.id, body)
          : action === "reject"
            ? await leaveApi.reject(selected.id, body)
            : await leaveApi.cancel(selected.id, body);

      setSelected(updated);
      setDecisionComment("");
      await loadData();
    } catch (err) {
      setError(`Leave request could not be ${action}d: ${(err as Error).message}`);
    } finally {
      setTransitioning(false);
    }
  }

  const activeEmployees = employees.filter((employee) => employee.status === "ACTIVE");

  return (
    <section className="page">
      <PageHeader
        eyebrow="HRMS"
        title="Leave"
        description="Tenant-scoped leave requests and permission-aware approval workflow."
        actions={
          canCreate && organizationId ? (
            <button
              type="button"
              className="button-primary"
              onClick={() => setShowCreate((current) => !current)}
            >
              {showCreate ? "Cancel" : "Create leave request"}
            </button>
          ) : undefined
        }
      />

      <HrmsOrganizationScope
        organizationId={organizationId}
        onChange={(value) => {
          setPage(0);
          setSelected(undefined);
          setOrganizationId(value);
        }}
      />

      {showCreate && canCreate && organizationId ? (
        <section className="panel">
          <h2>Create leave request</h2>
          <form onSubmit={handleCreate}>
            <label>
              Employee
              {canReadEmployees ? (
                <select
                  required
                  value={employeeId}
                  onChange={(event) => setEmployeeId(event.target.value)}
                  disabled={creating}
                >
                  <option value="">Select employee</option>
                  {activeEmployees.map((employee) => (
                    <option key={employee.id} value={employee.id}>
                      {employee.employeeCode}
                    </option>
                  ))}
                </select>
              ) : (
                <input
                  required
                  value={employeeId}
                  placeholder="Employee UUID"
                  onChange={(event) => setEmployeeId(event.target.value)}
                  disabled={creating}
                />
              )}
            </label>
            <label>
              Leave type
              <select
                value={leaveType}
                onChange={(event) => setLeaveType(event.target.value as LeaveType)}
                disabled={creating}
              >
                <option value="ANNUAL">ANNUAL</option>
                <option value="SICK">SICK</option>
                <option value="CASUAL">CASUAL</option>
                <option value="UNPAID">UNPAID</option>
              </select>
            </label>
            <label>
              Start date
              <input
                required
                type="date"
                value={startDate}
                onChange={(event) => setStartDate(event.target.value)}
                disabled={creating}
              />
            </label>
            <label>
              End date
              <input
                required
                type="date"
                min={startDate || undefined}
                value={endDate}
                onChange={(event) => setEndDate(event.target.value)}
                disabled={creating}
              />
            </label>
            <label>
              Reason
              <textarea
                value={reason}
                onChange={(event) => setReason(event.target.value)}
                disabled={creating}
              />
            </label>
            <button type="submit" className="button-primary" disabled={creating}>
              {creating ? "Creating..." : "Create leave request"}
            </button>
          </form>
        </section>
      ) : null}

      {!organizationId ? (
        <ErrorState message="Enter the tenant organization ID to load leave requests." />
      ) : null}
      {loading ? <LoadingState label="Loading leave requests" /> : null}
      {error ? <ErrorState message={error} /> : null}

      {organizationId && !loading ? (
        <>
          <DataTable
            rows={leaves.content as Array<Record<string, unknown>>}
            fallback="No leave requests found for this organization."
            onRowClick={(row) => {
              setSelected(row as unknown as LeaveResponse);
              setDecisionComment("");
            }}
            columns={[
              {
                key: "employeeId",
                label: "Employee",
                render: (row) =>
                  employeeCodes.get(String(row.employeeId ?? "")) ??
                  String(row.employeeId ?? "-"),
              },
              { key: "leaveType", label: "Type" },
              { key: "startDate", label: "Start" },
              { key: "endDate", label: "End" },
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
            totalElements={leaves.totalElements}
            onPageChange={(nextPage) => {
              setSelected(undefined);
              setPage(nextPage);
            }}
          />
        </>
      ) : null}

      {selected ? (
        <section className="panel">
          <h2>Leave request detail</h2>
          <ul className="detail-list">
            <li><strong>Employee:</strong> {employeeCodes.get(selected.employeeId) ?? selected.employeeId}</li>
            <li><strong>Type:</strong> {selected.leaveType}</li>
            <li><strong>Dates:</strong> {selected.startDate} to {selected.endDate}</li>
            <li><strong>Reason:</strong> {selected.reason || "-"}</li>
            <li><strong>Status:</strong> <StatusBadge status={selected.status} /></li>
            <li><strong>Requested by:</strong> {selected.requestedBy}</li>
            <li><strong>Decided by:</strong> {selected.decidedBy ?? "-"}</li>
            <li><strong>Decision comment:</strong> {selected.decisionComment || "-"}</li>
          </ul>

          {selected.status === "PENDING" && (canApprove || canCancel) ? (
            <>
              <label>
                Decision comment
                <textarea
                  maxLength={2000}
                  value={decisionComment}
                  onChange={(event) => setDecisionComment(event.target.value)}
                  disabled={transitioning}
                />
              </label>
              <div>
                {canApprove ? (
                  <>
                    <button
                      type="button"
                      className="button-primary"
                      disabled={transitioning}
                      onClick={() => void transition("approve")}
                    >
                      Approve
                    </button>{" "}
                    <button
                      type="button"
                      disabled={transitioning}
                      onClick={() => void transition("reject")}
                    >
                      Reject
                    </button>{" "}
                  </>
                ) : null}
                {canCancel ? (
                  <button
                    type="button"
                    disabled={transitioning}
                    onClick={() => void transition("cancel")}
                  >
                    Cancel request
                  </button>
                ) : null}
              </div>
            </>
          ) : null}
        </section>
      ) : null}
    </section>
  );
}
