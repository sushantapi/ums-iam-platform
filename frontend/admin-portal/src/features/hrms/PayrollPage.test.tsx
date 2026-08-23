// @vitest-environment jsdom

import "@testing-library/jest-dom/vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  employees: vi.fn(),
  runs: vi.fn(),
  salaryStructures: vi.fn(),
  createSalaryStructure: vi.fn(),
  createRun: vi.fn(),
  entries: vi.fn(),
  processRun: vi.fn(),
  finalizeRun: vi.fn(),
  payslip: vi.fn(),
  downloadPayslipPdf: vi.fn(),
}));

vi.mock("../../lib/auth/capabilities", () => ({
  hasAdminCapability: (capability: string) =>
    capability === "hrms.payroll.structure.manage" ||
    capability === "hrms.payroll.run.manage",
}));

vi.mock("./hrmsOrganizationScopeStorage", () => ({
  getStoredHrmsOrganizationId: () => "org-1",
  setStoredHrmsOrganizationId: vi.fn(),
}));

vi.mock("../../lib/api", () => ({
  hrmsApi: {
    employees: mocks.employees,
  },
}));

vi.mock("./payrollApi", () => ({
  payrollApi: {
    runs: mocks.runs,
    salaryStructures: mocks.salaryStructures,
    createSalaryStructure: mocks.createSalaryStructure,
    createRun: mocks.createRun,
    entries: mocks.entries,
    processRun: mocks.processRun,
    finalizeRun: mocks.finalizeRun,
    payslip: mocks.payslip,
    downloadPayslipPdf: mocks.downloadPayslipPdf,
  },
}));

import { PayrollPage } from "./PayrollPage";

const employee = {
  id: "emp-1",
  umsUserId: "user-1",
  organizationId: "org-1",
  employeeCode: "EMP-001",
  departmentId: null,
  designationId: null,
  status: "ACTIVE",
  createdAt: "2026-08-20T09:00:00",
  updatedAt: "2026-08-20T09:00:00",
};

const draftRun = {
  id: "run-1",
  organizationId: "org-1",
  payrollMonth: "2026-08",
  status: "DRAFT",
  createdBy: "user-1",
  processedBy: null,
  processedAt: null,
  finalizedBy: null,
  finalizedAt: null,
  createdAt: "2026-08-20T09:00:00",
  updatedAt: "2026-08-20T09:00:00",
};

const processedRun = {
  ...draftRun,
  status: "PROCESSED",
  processedBy: "user-1",
  processedAt: "2026-08-20T10:00:00",
};

const finalizedRun = {
  ...processedRun,
  status: "FINALIZED",
  finalizedBy: "user-1",
  finalizedAt: "2026-08-20T11:00:00",
};

const entry = {
  id: "entry-1",
  payrollRunId: "run-1",
  organizationId: "org-1",
  employeeId: "emp-1",
  salaryStructureId: "structure-1",
  basicPay: 50000,
  allowanceTotal: 10000,
  grossPay: 60000,
  configuredDeductionTotal: 701.11,
  pfContributionWage: 15000,
  employeePfContribution: 1800.12,
  employerPfContribution: 1800.34,
  esiContributionWage: 18000,
  employeeEsiContribution: 135.56,
  employerEsiContribution: 585.78,
  tdsAmount: 499.99,
  statutoryEmployeeDeductionTotal: 2222.22,
  employerStatutoryContributionTotal: 3333.33,
  statutoryPolicyId: "policy-1",
  statutoryPolicyVersion: "IN-2026.1",
  taxRegime: "NEW",
  deductionTotal: 4444.44,
  netPay: 55555.55,
  generatedAt: "2026-08-20T10:00:00",
};

function employeePage() {
  return {
    content: [employee],
    page: 0,
    size: 100,
    totalElements: 1,
    totalPages: 1,
  };
}

describe("PayrollPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.employees.mockResolvedValue(employeePage());
    mocks.salaryStructures.mockResolvedValue([]);
    mocks.entries.mockResolvedValue([]);
    mocks.payslip.mockResolvedValue(entry);
    mocks.downloadPayslipPdf.mockResolvedValue({
      blob: new Blob(["pdf"], { type: "application/pdf" }),
      filename: "payslip-EMP-001-2026-08.pdf",
    });
    vi.stubGlobal("URL", {
      ...URL,
      createObjectURL: vi.fn(() => "blob:payslip"),
      revokeObjectURL: vi.fn(),
    });
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it("offers process only for a DRAFT payroll run", async () => {
    mocks.runs.mockResolvedValue([draftRun]);
    mocks.processRun.mockResolvedValue(processedRun);

    render(<PayrollPage />);

    const monthCell = await screen.findByText("2026-08");
    fireEvent.click(monthCell.closest("tr")!);

    expect(
      await screen.findByRole("button", { name: "Process run" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Finalize run" }),
    ).not.toBeInTheDocument();
  });

  it("offers finalize for PROCESSED and renders the backend payslip snapshot", async () => {
    mocks.runs.mockResolvedValue([processedRun]);
    mocks.entries.mockResolvedValue([entry]);
    mocks.finalizeRun.mockResolvedValue(finalizedRun);

    render(<PayrollPage />);

    const monthCell = await screen.findByText("2026-08");
    fireEvent.click(monthCell.closest("tr")!);

    expect(
      await screen.findByRole("button", { name: "Finalize run" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Process run" }),
    ).not.toBeInTheDocument();

    const grossCell = await screen.findByText(/60,000/);
    fireEvent.click(grossCell.closest("tr")!);

    expect(await screen.findByText("Payslip snapshot")).toBeInTheDocument();

    const configuredDeductions = screen
      .getByText("Configured / other deductions:")
      .closest("li");
    expect(configuredDeductions).toHaveTextContent(/701\.11/);

    const employeePf = screen.getByText("Employee PF:").closest("li");
    expect(employeePf).toHaveTextContent(/1,800\.12/);

    const employerPf = screen.getByText("Employer PF:").closest("li");
    expect(employerPf).toHaveTextContent(/1,800\.34/);

    const employeeEsi = screen.getByText("Employee ESI:").closest("li");
    expect(employeeEsi).toHaveTextContent(/135\.56/);

    const employerEsi = screen.getByText("Employer ESI:").closest("li");
    expect(employerEsi).toHaveTextContent(/585\.78/);

    const tds = screen.getByText("TDS:").closest("li");
    expect(tds).toHaveTextContent(/499\.99/);

    const statutoryDeductions = screen
      .getByText("Statutory employee deductions:")
      .closest("li");
    expect(statutoryDeductions).toHaveTextContent(/2,222\.22/);

    const totalDeductions = screen.getByText("Total deductions:").closest("li");
    expect(totalDeductions).toHaveTextContent(/4,444\.44/);

    const netPay = screen.getByText("Net:").closest("li");
    expect(netPay).toHaveTextContent(/55,555\.55/);

    const employerTotal = screen
      .getByText("Employer statutory total:")
      .closest("li");
    expect(employerTotal).toHaveTextContent(/3,333\.33/);

    expect(screen.getByText("IN-2026.1")).toBeInTheDocument();
    expect(screen.getByText("NEW")).toBeInTheDocument();

    expect(screen.queryByRole("button", { name: "Download PDF" })).not.toBeInTheDocument();
    expect(mocks.payslip).toHaveBeenCalledWith("entry-1", "org-1");

    fireEvent.click(screen.getByRole("button", { name: "Finalize run" }));
    await waitFor(() =>
      expect(mocks.finalizeRun).toHaveBeenCalledWith("run-1", "org-1"),
    );
  });

  it("renders a legacy non-statutory payslip snapshot without recomputing", async () => {
    const legacyEntry = {
      ...entry,
      configuredDeductionTotal: 5000,
      pfContributionWage: 0,
      employeePfContribution: 0,
      employerPfContribution: 0,
      esiContributionWage: 0,
      employeeEsiContribution: 0,
      employerEsiContribution: 0,
      tdsAmount: 0,
      statutoryEmployeeDeductionTotal: 0,
      employerStatutoryContributionTotal: 0,
      statutoryPolicyId: null,
      statutoryPolicyVersion: null,
      taxRegime: null,
      deductionTotal: 5000,
      netPay: 55000,
    };

    mocks.runs.mockResolvedValue([processedRun]);
    mocks.entries.mockResolvedValue([legacyEntry]);
    mocks.payslip.mockResolvedValue(legacyEntry);

    render(<PayrollPage />);

    const monthCell = await screen.findByText("2026-08");
    fireEvent.click(monthCell.closest("tr")!);

    const grossCell = await screen.findByText(/60,000/);
    fireEvent.click(grossCell.closest("tr")!);

    expect(await screen.findByText("Payslip snapshot")).toBeInTheDocument();

    const configured = screen
      .getByText("Configured / other deductions:")
      .closest("li");
    expect(configured).toHaveTextContent(/5,000/);

    const employeePf = screen.getByText("Employee PF:").closest("li");
    expect(employeePf).toHaveTextContent(/0\.00/);

    const employeeEsi = screen.getByText("Employee ESI:").closest("li");
    expect(employeeEsi).toHaveTextContent(/0\.00/);

    const tds = screen.getByText("TDS:").closest("li");
    expect(tds).toHaveTextContent(/0\.00/);

    const policy = screen.getByText("Statutory policy:").closest("li");
    expect(policy).toHaveTextContent("Not applicable");

    const regime = screen.getByText("Tax regime:").closest("li");
    expect(regime).toHaveTextContent("Not specified");

    const totalDeductions = screen.getByText("Total deductions:").closest("li");
    expect(totalDeductions).toHaveTextContent(/5,000/);

    const net = screen.getByText("Net:").closest("li");
    expect(net).toHaveTextContent(/55,000/);

    expect(mocks.payslip).toHaveBeenCalledWith("entry-1", "org-1");
  });

  it("downloads PDF only for a FINALIZED payroll run", async () => {
    mocks.runs.mockResolvedValue([finalizedRun]);
    mocks.entries.mockResolvedValue([entry]);
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});

    render(<PayrollPage />);

    const monthCell = await screen.findByText("2026-08");
    fireEvent.click(monthCell.closest("tr")!);

    const grossCell = await screen.findByText(/60,000/);
    fireEvent.click(grossCell.closest("tr")!);

    const download = await screen.findByRole("button", { name: "Download PDF" });
    fireEvent.click(download);

    await waitFor(() =>
      expect(mocks.downloadPayslipPdf).toHaveBeenCalledWith("entry-1", "org-1"),
    );
    expect(URL.createObjectURL).toHaveBeenCalled();
    expect(clickSpy).toHaveBeenCalled();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith("blob:payslip");

    clickSpy.mockRestore();
  });
});
