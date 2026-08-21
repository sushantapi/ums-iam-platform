// @vitest-environment jsdom

import "@testing-library/jest-dom/vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  navigate: vi.fn(),
  employees: vi.fn(),
  departments: vi.fn(),
  designations: vi.fn(),
  createEmployee: vi.fn(),
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual<typeof import("react-router-dom")>(
    "react-router-dom",
  );
  return {
    ...actual,
    useNavigate: () => mocks.navigate,
  };
});

vi.mock("../../lib/auth/capabilities", () => ({
  hasAdminCapability: (capability: string) =>
    capability === "hrms.employees.create",
}));

vi.mock("./hrmsOrganizationScopeStorage", () => ({
  getStoredHrmsOrganizationId: () => "org-1",
  setStoredHrmsOrganizationId: vi.fn(),
}));

vi.mock("../../lib/api", () => ({
  hrmsApi: {
    employees: mocks.employees,
    departments: mocks.departments,
    designations: mocks.designations,
    createEmployee: mocks.createEmployee,
  },
}));

import { EmployeesPage } from "./EmployeesPage";

const employee = {
  id: "emp-1",
  umsUserId: "user-1",
  organizationId: "org-1",
  employeeCode: "EMP-001",
  departmentId: "dep-1",
  designationId: "des-1",
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
    size: 20,
    totalElements: content.length,
    totalPages: content.length > 0 ? 1 : 0,
  };
}

describe("EmployeesPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.employees.mockResolvedValue(page([employee]));
    mocks.departments.mockResolvedValue(page([department]));
    mocks.designations.mockResolvedValue(page([designation]));
    mocks.createEmployee.mockResolvedValue({
      ...employee,
      id: "emp-2",
      umsUserId: "user-2",
      employeeCode: "EMP-002",
    });
  });

  afterEach(cleanup);

  it("loads tenant-scoped employees with department and designation labels", async () => {
    render(<EmployeesPage />);

    expect(await screen.findByText("EMP-001")).toBeInTheDocument();
    expect(screen.getByText("Engineering")).toBeInTheDocument();
    expect(screen.getByText("Software Engineer")).toBeInTheDocument();

    expect(mocks.employees).toHaveBeenCalledWith({
      organizationId: "org-1",
      page: 0,
      size: 20,
    });
  });

  it("creates an employee using the selected same-tenant master data", async () => {
    render(<EmployeesPage />);
    await screen.findByText("EMP-001");

    fireEvent.click(screen.getByRole("button", { name: "Create employee" }));
    fireEvent.change(screen.getByLabelText("UMS user ID"), {
      target: { value: "user-2" },
    });
    fireEvent.change(screen.getByLabelText("Employee code"), {
      target: { value: "EMP-002" },
    });
    fireEvent.change(screen.getByLabelText("Department"), {
      target: { value: "dep-1" },
    });
    fireEvent.change(screen.getByLabelText("Designation"), {
      target: { value: "des-1" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Create employee" }));

    await waitFor(() =>
      expect(mocks.createEmployee).toHaveBeenCalledWith({
        organizationId: "org-1",
        umsUserId: "user-2",
        employeeCode: "EMP-002",
        departmentId: "dep-1",
        designationId: "des-1",
      }),
    );
    expect(mocks.navigate).toHaveBeenCalledWith("/hrms/employees/emp-2");
  });
});
