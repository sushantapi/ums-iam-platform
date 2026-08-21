// @vitest-environment jsdom

import "@testing-library/jest-dom/vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  employees: vi.fn(),
  list: vi.fn(),
  create: vi.fn(),
  update: vi.fn(),
}));

vi.mock("../../lib/auth/capabilities", () => ({
  hasAdminCapability: (capability: string) =>
    capability === "hrms.attendance.create" ||
    capability === "hrms.attendance.update",
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

vi.mock("./attendanceApi", () => ({
  attendanceApi: {
    list: mocks.list,
    create: mocks.create,
    update: mocks.update,
  },
}));

import { AttendancePage } from "./AttendancePage";

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

const attendanceRecord = {
  id: "att-1",
  organizationId: "org-1",
  employeeId: "emp-1",
  workDate: "2026-08-20",
  status: "PRESENT",
  checkInAt: "2026-08-20T09:00:00",
  checkOutAt: null,
  notes: "Office",
  createdBy: "user-1",
  createdAt: "2026-08-20T09:00:00",
  updatedAt: "2026-08-20T09:00:00",
};

function employeePage() {
  return {
    content: [employee],
    page: 0,
    size: 200,
    totalElements: 1,
    totalPages: 1,
  };
}

function attendancePage() {
  return {
    content: [attendanceRecord],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
  };
}

describe("AttendancePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.employees.mockResolvedValue(employeePage());
    mocks.list.mockResolvedValue(attendancePage());
    mocks.create.mockResolvedValue(attendanceRecord);
    mocks.update.mockResolvedValue({
      ...attendanceRecord,
      status: "HALF_DAY",
      notes: "Updated",
    });
  });

  afterEach(cleanup);

  it("creates and updates tenant-scoped attendance", async () => {
    render(<AttendancePage />);
    expect(await screen.findByText("2026-08-20")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Create attendance" }));
    fireEvent.change(screen.getByLabelText("Employee"), {
      target: { value: "emp-1" },
    });
    fireEvent.change(screen.getByLabelText("Work date"), {
      target: { value: "2026-08-21" },
    });
    fireEvent.change(screen.getByLabelText("Notes"), {
      target: { value: "Created from portal" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Create" }));

    await waitFor(() =>
      expect(mocks.create).toHaveBeenCalledWith({
        organizationId: "org-1",
        employeeId: "emp-1",
        workDate: "2026-08-21",
        status: "PRESENT",
        checkInAt: null,
        checkOutAt: null,
        notes: "Created from portal",
      }),
    );

    fireEvent.click(screen.getByText("2026-08-20"));
    expect(screen.getByRole("heading", { name: "Update attendance" })).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Status"), {
      target: { value: "HALF_DAY" },
    });
    fireEvent.change(screen.getByLabelText("Notes"), {
      target: { value: "Updated" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Update" }));

    await waitFor(() =>
      expect(mocks.update).toHaveBeenCalledWith("att-1", {
        organizationId: "org-1",
        status: "HALF_DAY",
        checkInAt: "2026-08-20T09:00:00",
        checkOutAt: null,
        notes: "Updated",
      }),
    );
  });

  it("surfaces a duplicate attendance API error", async () => {
    mocks.create.mockRejectedValueOnce(
      new Error("Attendance already exists for employee and work date"),
    );

    render(<AttendancePage />);
    await screen.findByText("2026-08-20");

    fireEvent.click(screen.getByRole("button", { name: "Create attendance" }));
    fireEvent.change(screen.getByLabelText("Employee"), {
      target: { value: "emp-1" },
    });
    fireEvent.change(screen.getByLabelText("Work date"), {
      target: { value: "2026-08-20" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Create" }));

    expect(
      await screen.findByText(
        "Attendance could not be saved: Attendance already exists for employee and work date",
      ),
    ).toBeInTheDocument();
  });
});
