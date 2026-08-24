// @vitest-environment jsdom

import "@testing-library/jest-dom/vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  navigate: vi.fn(),
  employeeDetail: vi.fn(),
  departments: vi.fn(),
  designations: vi.fn(),
  updateEmployee: vi.fn(),
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual<typeof import("react-router-dom")>(
    "react-router-dom",
  );
  return {
    ...actual,
    useNavigate: () => mocks.navigate,
    useParams: () => ({ employeeId: "emp-1" }),
  };
});

vi.mock("../../lib/auth/capabilities", () => ({
  hasAdminCapability: (capability: string) =>
    capability === "hrms.employees.update",
}));

vi.mock("./hrmsOrganizationScopeStorage", () => ({
  getStoredHrmsOrganizationId: () => "org-1",
}));

vi.mock("../../lib/api", () => ({
  hrmsApi: {
    employeeDetail: mocks.employeeDetail,
    departments: mocks.departments,
    designations: mocks.designations,
    updateEmployee: mocks.updateEmployee,
  },
}));

import { EmployeeDetailPage } from "./EmployeeDetailPage";

const employee = {
  id: "emp-1",
  umsUserId: "user-1",
  organizationId: "org-1",
  employeeCode: "EMP-001",
  departmentId: "dep-1",
  designationId: "des-1",
  displayName: "Sushant Kumar",
  dateOfJoining: "2025-09-24",
  panDisplay: "******234F",
  uanDisplay: "********0400",
  esiDisplay: "******7890",
  bankAccountDisplay: "********7890",
  status: "ACTIVE",
  createdAt: "2026-08-20T09:00:00",
  updatedAt: "2026-08-20T09:00:00",
};

const department = {
  id: "dep-1",
  organizationId: "org-1",
  code: "ENG",
  name: "Engineering",
  description: null,
  status: "ACTIVE",
  createdAt: "2026-08-20T09:00:00",
  updatedAt: "2026-08-20T09:00:00",
};

const designation = {
  id: "des-1",
  organizationId: "org-1",
  code: "SDE",
  name: "Software Engineer",
  description: null,
  status: "ACTIVE",
  createdAt: "2026-08-20T09:00:00",
  updatedAt: "2026-08-20T09:00:00",
};

function page<T>(content: T[]) {
  return {
    content,
    page: 0,
    size: 100,
    totalElements: content.length,
    totalPages: content.length > 0 ? 1 : 0,
  };
}

describe("EmployeeDetailPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.employeeDetail.mockResolvedValue(employee);
    mocks.departments.mockResolvedValue(page([department]));
    mocks.designations.mockResolvedValue(page([designation]));
    mocks.updateEmployee.mockResolvedValue(employee);
  });

  afterEach(cleanup);

  it("shows masked reusable payslip identity fields", async () => {
    render(<EmployeeDetailPage />);

    expect(await screen.findByText("Sushant Kumar")).toBeInTheDocument();
    expect(screen.getByText("******234F")).toBeInTheDocument();
    expect(screen.getByText("********0400")).toBeInTheDocument();
    expect(screen.getByText("******7890")).toBeInTheDocument();
    expect(screen.getByText("********7890")).toBeInTheDocument();
    expect(screen.getByText("Engineering")).toBeInTheDocument();
    expect(screen.getByText("Software Engineer")).toBeInTheDocument();
  });

  it("sends only explicit raw sensitive replacements while keeping masked values hidden", async () => {
    render(<EmployeeDetailPage />);
    await screen.findByText("Sushant Kumar");

    fireEvent.click(screen.getByRole("button", { name: "Edit employee" }));
    expect(screen.getByLabelText("Replace PAN number")).toHaveValue("");
    expect(screen.getByLabelText("Replace UAN number")).toHaveValue("");

    fireEvent.change(screen.getByLabelText("Replace PAN number"), {
      target: { value: "ABCDE9999Z" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save changes" }));

    await waitFor(() =>
      expect(mocks.updateEmployee).toHaveBeenCalledWith(
        "emp-1",
        expect.objectContaining({
          organizationId: "org-1",
          employeeCode: "EMP-001",
          displayName: "Sushant Kumar",
          dateOfJoining: "2025-09-24",
          panNumber: "ABCDE9999Z",
          uanNumber: undefined,
          esiNumber: undefined,
          bankAccountNumber: undefined,
        }),
      ),
    );
  });
});
