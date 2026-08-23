import { FormEvent, useEffect, useMemo, useState } from "react";
import { DataTable } from "../../components/ui/DataTable";
import { ErrorState } from "../../components/ui/ErrorState";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { StatusBadge } from "../../components/ui/StatusBadge";
import {
  hrmsApi,
  type EmployeeResponse,
} from "../../lib/api";
import { hasAdminCapability } from "../../lib/auth/capabilities";
import { HrmsOrganizationScope } from "./HrmsOrganizationScope";
import { getStoredHrmsOrganizationId } from "./hrmsOrganizationScopeStorage";
import {
  payrollApi,
  type PayrollEntryResponse,
  type PayrollRunResponse,
  type SalaryStructureResponse,
} from "./payrollApi";

function currentMonth(): string {
  return new Date().toISOString().slice(0, 7);
}

function formatMoney(value: number, currency = "INR"): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency,
    maximumFractionDigits: 2,
  }).format(value);
}

export function PayrollPage() {
  const canManageStructures = hasAdminCapability(
    "hrms.payroll.structure.manage",
  );
  const canManageRuns = hasAdminCapability("hrms.payroll.run.manage");
  const [organizationId, setOrganizationId] = useState(
    getStoredHrmsOrganizationId(),
  );
  const [employees, setEmployees] = useState<EmployeeResponse[]>([]);
  const [runs, setRuns] = useState<PayrollRunResponse[]>([]);
  const [selectedEmployeeId, setSelectedEmployeeId] = useState("");
  const [structures, setStructures] = useState<SalaryStructureResponse[]>([]);
  const [selectedRun, setSelectedRun] = useState<PayrollRunResponse>();
  const [entries, setEntries] = useState<PayrollEntryResponse[]>([]);
  const [payslip, setPayslip] = useState<PayrollEntryResponse>();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>();
  const [savingStructure, setSavingStructure] = useState(false);
  const [savingRun, setSavingRun] = useState(false);
  const [transitioning, setTransitioning] = useState(false);
  const [downloadingPayslip, setDownloadingPayslip] = useState(false);
  const [currency, setCurrency] = useState("INR");
  const [basicPay, setBasicPay] = useState("50000");
  const [allowanceTotal, setAllowanceTotal] = useState("0");
  const [deductionTotal, setDeductionTotal] = useState("0");
  const [effectiveFrom, setEffectiveFrom] = useState(
    new Date().toISOString().slice(0, 10),
  );
  const [effectiveTo, setEffectiveTo] = useState("");
  const [payrollMonth, setPayrollMonth] = useState(currentMonth());

  useEffect(() => {
    setSelectedEmployeeId("");
    setStructures([]);
    setSelectedRun(undefined);
    setEntries([]);
    setPayslip(undefined);

    if (!organizationId) {
      setEmployees([]);
      setRuns([]);
      return;
    }

    setLoading(true);
    setError(undefined);
    Promise.all([
      hrmsApi.employees({ organizationId, page: 0, size: 100 }),
      payrollApi.runs(organizationId),
    ])
      .then(([employeePage, payrollRuns]) => {
        setEmployees(employeePage.content);
        setRuns(payrollRuns);
      })
      .catch((err: Error) =>
        setError(`Payroll data could not be loaded: ${err.message}`),
      )
      .finally(() => setLoading(false));
  }, [organizationId]);

  useEffect(() => {
    if (!organizationId || !selectedEmployeeId) {
      setStructures([]);
      return;
    }

    setError(undefined);
    payrollApi
      .salaryStructures(organizationId, selectedEmployeeId)
      .then(setStructures)
      .catch((err: Error) =>
        setError(`Salary structures could not be loaded: ${err.message}`),
      );
  }, [organizationId, selectedEmployeeId]);

  const employeeCodes = useMemo(
    () => new Map(employees.map((employee) => [employee.id, employee.employeeCode])),
    [employees],
  );

  async function refreshRuns(selectId?: string) {
    if (!organizationId) {
      return;
    }
    const refreshed = await payrollApi.runs(organizationId);
    setRuns(refreshed);
    if (selectId) {
      const match = refreshed.find((run) => run.id === selectId);
      setSelectedRun(match);
    }
  }

  async function refreshStructures() {
    if (!organizationId || !selectedEmployeeId) {
      return;
    }
    setStructures(
      await payrollApi.salaryStructures(organizationId, selectedEmployeeId),
    );
  }

  async function createSalaryStructure(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!organizationId || !selectedEmployeeId) {
      setError("Select an organization and employee first.");
      return;
    }

    setSavingStructure(true);
    setError(undefined);
    try {
      await payrollApi.createSalaryStructure({
        organizationId,
        employeeId: selectedEmployeeId,
        currency: currency.trim().toUpperCase(),
        basicPay: Number(basicPay),
        allowanceTotal: Number(allowanceTotal),
        deductionTotal: Number(deductionTotal),
        effectiveFrom,
        effectiveTo: effectiveTo || null,
        active: true,
      });
      await refreshStructures();
    } catch (err) {
      setError(`Salary structure could not be created: ${(err as Error).message}`);
    } finally {
      setSavingStructure(false);
    }
  }

  async function createPayrollRun(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!organizationId) {
      return;
    }

    setSavingRun(true);
    setError(undefined);
    try {
      const created = await payrollApi.createRun(organizationId, payrollMonth);
      await refreshRuns(created.id);
      setEntries([]);
      setPayslip(undefined);
    } catch (err) {
      setError(`Payroll run could not be created: ${(err as Error).message}`);
    } finally {
      setSavingRun(false);
    }
  }

  async function selectRun(run: PayrollRunResponse) {
    if (!organizationId) {
      return;
    }
    setSelectedRun(run);
    setPayslip(undefined);
    setError(undefined);
    try {
      setEntries(await payrollApi.entries(run.id, organizationId));
    } catch (err) {
      setEntries([]);
      setError(`Payroll entries could not be loaded: ${(err as Error).message}`);
    }
  }

  async function transitionRun(action: "process" | "finalize") {
    if (!organizationId || !selectedRun) {
      return;
    }

    setTransitioning(true);
    setError(undefined);
    try {
      const updated =
        action === "process"
          ? await payrollApi.processRun(selectedRun.id, organizationId)
          : await payrollApi.finalizeRun(selectedRun.id, organizationId);
      await refreshRuns(updated.id);
      setEntries(await payrollApi.entries(updated.id, organizationId));
    } catch (err) {
      setError(`Payroll run could not be ${action}d: ${(err as Error).message}`);
    } finally {
      setTransitioning(false);
    }
  }

  async function loadPayslip(entry: PayrollEntryResponse) {
    if (!organizationId) {
      return;
    }
    setError(undefined);
    try {
      setPayslip(await payrollApi.payslip(entry.id, organizationId));
    } catch (err) {
      setError(`Payslip could not be loaded: ${(err as Error).message}`);
    }
  }

  async function downloadPayslipPdf() {
    if (!organizationId || !payslip || selectedRun?.status !== "FINALIZED") {
      return;
    }

    setDownloadingPayslip(true);
    setError(undefined);
    try {
      const { blob, filename } = await payrollApi.downloadPayslipPdf(
        payslip.id,
        organizationId,
      );
      const objectUrl = URL.createObjectURL(blob);
      try {
        const anchor = document.createElement("a");
        anchor.href = objectUrl;
        anchor.download =
          filename ??
          `payslip-${employeeCodes.get(payslip.employeeId) ?? payslip.employeeId}-${selectedRun.payrollMonth}.pdf`;
        document.body.appendChild(anchor);
        anchor.click();
        anchor.remove();
      } finally {
        URL.revokeObjectURL(objectUrl);
      }
    } catch (err) {
      setError(`Payslip PDF could not be downloaded: ${(err as Error).message}`);
    } finally {
      setDownloadingPayslip(false);
    }
  }

  return (
    <section className="page">
      <PageHeader
        eyebrow="HRMS"
        title="Payroll"
        description="Manage tenant-scoped salary structures, payroll runs, immutable entries, and payslip snapshots through the IAM Gateway."
      />

      <HrmsOrganizationScope
        organizationId={organizationId}
        onChange={setOrganizationId}
      />

      {!organizationId ? (
        <ErrorState message="Enter the tenant organization ID to load payroll." />
      ) : null}
      {loading ? <LoadingState label="Loading payroll" /> : null}
      {error ? <ErrorState message={error} /> : null}

      {organizationId && !loading ? (
        <>
          <section className="panel">
            <h2>Salary structures</h2>
            <label>
              Employee
              <select
                value={selectedEmployeeId}
                onChange={(event) => setSelectedEmployeeId(event.target.value)}
              >
                <option value="">Select employee</option>
                {employees.map((employee) => (
                  <option key={employee.id} value={employee.id}>
                    {employee.employeeCode} ({employee.status})
                  </option>
                ))}
              </select>
            </label>

            {canManageStructures && selectedEmployeeId ? (
              <form onSubmit={createSalaryStructure}>
                <label>
                  Currency
                  <input
                    required
                    maxLength={3}
                    value={currency}
                    onChange={(event) => setCurrency(event.target.value)}
                    disabled={savingStructure}
                  />
                </label>
                <label>
                  Basic pay
                  <input
                    required
                    type="number"
                    min="0"
                    step="0.01"
                    value={basicPay}
                    onChange={(event) => setBasicPay(event.target.value)}
                    disabled={savingStructure}
                  />
                </label>
                <label>
                  Allowance total
                  <input
                    required
                    type="number"
                    min="0"
                    step="0.01"
                    value={allowanceTotal}
                    onChange={(event) => setAllowanceTotal(event.target.value)}
                    disabled={savingStructure}
                  />
                </label>
                <label>
                  Deduction total
                  <input
                    required
                    type="number"
                    min="0"
                    step="0.01"
                    value={deductionTotal}
                    onChange={(event) => setDeductionTotal(event.target.value)}
                    disabled={savingStructure}
                  />
                </label>
                <label>
                  Effective from
                  <input
                    required
                    type="date"
                    value={effectiveFrom}
                    onChange={(event) => setEffectiveFrom(event.target.value)}
                    disabled={savingStructure}
                  />
                </label>
                <label>
                  Effective to
                  <input
                    type="date"
                    value={effectiveTo}
                    onChange={(event) => setEffectiveTo(event.target.value)}
                    disabled={savingStructure}
                  />
                </label>
                <button
                  type="submit"
                  className="button-primary"
                  disabled={savingStructure}
                >
                  {savingStructure ? "Creating..." : "Create salary structure"}
                </button>
              </form>
            ) : null}

            {selectedEmployeeId ? (
              <DataTable
                rows={structures as Array<Record<string, unknown>>}
                fallback="No salary structures found for this employee."
                columns={[
                  { key: "currency", label: "Currency" },
                  {
                    key: "basicPay",
                    label: "Basic pay",
                    render: (row) =>
                      formatMoney(Number(row.basicPay), String(row.currency ?? "INR")),
                  },
                  {
                    key: "allowanceTotal",
                    label: "Allowances",
                    render: (row) =>
                      formatMoney(
                        Number(row.allowanceTotal),
                        String(row.currency ?? "INR"),
                      ),
                  },
                  {
                    key: "deductionTotal",
                    label: "Deductions",
                    render: (row) =>
                      formatMoney(
                        Number(row.deductionTotal),
                        String(row.currency ?? "INR"),
                      ),
                  },
                  { key: "effectiveFrom", label: "Effective from" },
                  { key: "effectiveTo", label: "Effective to" },
                  {
                    key: "active",
                    label: "Status",
                    render: (row) => (
                      <StatusBadge status={row.active ? "ACTIVE" : "INACTIVE"} />
                    ),
                  },
                ]}
              />
            ) : null}
          </section>

          <section className="panel">
            <h2>Payroll runs</h2>
            {canManageRuns ? (
              <form onSubmit={createPayrollRun}>
                <label>
                  Payroll month
                  <input
                    required
                    type="month"
                    value={payrollMonth}
                    onChange={(event) => setPayrollMonth(event.target.value)}
                    disabled={savingRun}
                  />
                </label>
                <button
                  type="submit"
                  className="button-primary"
                  disabled={savingRun}
                >
                  {savingRun ? "Creating..." : "Create payroll run"}
                </button>
              </form>
            ) : null}

            <DataTable
              rows={runs as Array<Record<string, unknown>>}
              fallback="No payroll runs found for this organization."
              onRowClick={(row) => {
                const run = runs.find((item) => item.id === String(row.id));
                if (run) {
                  void selectRun(run);
                }
              }}
              columns={[
                { key: "payrollMonth", label: "Month" },
                {
                  key: "status",
                  label: "Status",
                  render: (row) => (
                    <StatusBadge status={String(row.status ?? "Unknown")} />
                  ),
                },
                { key: "processedAt", label: "Processed at" },
                { key: "finalizedAt", label: "Finalized at" },
              ]}
            />
          </section>

          {selectedRun ? (
            <section className="panel">
              <h2>Run {selectedRun.payrollMonth}</h2>
              <ul className="detail-list">
                <li><strong>Run ID:</strong> {selectedRun.id}</li>
                <li><strong>Status:</strong> <StatusBadge status={selectedRun.status} /></li>
              </ul>
              {canManageRuns && selectedRun.status === "DRAFT" ? (
                <button
                  type="button"
                  className="button-primary"
                  disabled={transitioning}
                  onClick={() => void transitionRun("process")}
                >
                  {transitioning ? "Processing..." : "Process run"}
                </button>
              ) : null}
              {canManageRuns && selectedRun.status === "PROCESSED" ? (
                <button
                  type="button"
                  className="button-primary"
                  disabled={transitioning}
                  onClick={() => void transitionRun("finalize")}
                >
                  {transitioning ? "Finalizing..." : "Finalize run"}
                </button>
              ) : null}

              <DataTable
                rows={entries as Array<Record<string, unknown>>}
                fallback={
                  selectedRun.status === "DRAFT"
                    ? "Process this run to generate payroll entries."
                    : "No payroll entries were generated."
                }
                onRowClick={(row) => {
                  const entry = entries.find((item) => item.id === String(row.id));
                  if (entry) {
                    void loadPayslip(entry);
                  }
                }}
                columns={[
                  {
                    key: "employeeId",
                    label: "Employee",
                    render: (row) =>
                      employeeCodes.get(String(row.employeeId)) ?? String(row.employeeId),
                  },
                  {
                    key: "grossPay",
                    label: "Gross",
                    render: (row) => formatMoney(Number(row.grossPay)),
                  },
                  {
                    key: "deductionTotal",
                    label: "Deductions",
                    render: (row) => formatMoney(Number(row.deductionTotal)),
                  },
                  {
                    key: "netPay",
                    label: "Net",
                    render: (row) => formatMoney(Number(row.netPay)),
                  },
                  { key: "generatedAt", label: "Generated at" },
                ]}
              />
            </section>
          ) : null}

          {payslip ? (
            <section className="panel">
              <h2>Payslip snapshot</h2>
              <ul className="detail-list">
                <li><strong>Entry ID:</strong> {payslip.id}</li>
                <li><strong>Employee:</strong> {employeeCodes.get(payslip.employeeId) ?? payslip.employeeId}</li>
                <li><strong>Salary structure:</strong> {payslip.salaryStructureId}</li>
                <li><strong>Basic:</strong> {formatMoney(payslip.basicPay)}</li>
                <li><strong>Allowances:</strong> {formatMoney(payslip.allowanceTotal)}</li>
                <li><strong>Gross:</strong> {formatMoney(payslip.grossPay)}</li>
                <li><strong>Deductions:</strong> {formatMoney(payslip.deductionTotal)}</li>
                <li><strong>Net:</strong> {formatMoney(payslip.netPay)}</li>
                <li><strong>Generated:</strong> {payslip.generatedAt}</li>
              </ul>
              {selectedRun?.status === "FINALIZED" ? (
                <button
                  type="button"
                  className="button-primary"
                  disabled={downloadingPayslip}
                  onClick={() => void downloadPayslipPdf()}
                >
                  {downloadingPayslip ? "Downloading..." : "Download PDF"}
                </button>
              ) : null}
            </section>
          ) : null}
        </>
      ) : null}
    </section>
  );
}
