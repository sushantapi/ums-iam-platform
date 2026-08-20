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

const entry = {
  id: "entry-1",
  payrollRunId: "run-1",
  organizationId: "org-1",
  employeeId: "emp-1",
  salaryStructureId: "structure-1",
  basicPay: 50000,
  allowanceTotal: 10000,
  grossPay: 60000,
  deductionTotal: 5000,
  netPay: 55000,
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
  });

  afterEach(cleanup);

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
    mocks.finalizeRun.mockResolvedValue({
      ...processedRun,
      status: "FINALIZED",
      finalizedBy: "user-1",
      finalizedAt: "2026-08-20T11:00:00",
    });

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
    expect(screen.getAllByText(/55,000/).length).toBeGreaterThan(0);
    expect(mocks.payslip).toHaveBeenCalledWith("entry-1", "org-1");

    fireEvent.click(screen.getByRole("button", { name: "Finalize run" }));
    await waitFor(() =>
      expect(mocks.finalizeRun).toHaveBeenCalledWith("run-1", "org-1"),
    );
  });
});
