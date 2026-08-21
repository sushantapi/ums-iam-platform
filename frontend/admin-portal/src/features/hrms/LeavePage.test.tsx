// @vitest-environment jsdom

import "@testing-library/jest-dom/vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { LeaveResponse } from "./leaveApi";

const mocks = vi.hoisted(() => ({
  employees: vi.fn(),
  list: vi.fn(),
  create: vi.fn(),
  approve: vi.fn(),
  reject: vi.fn(),
  cancel: vi.fn(),
}));

vi.mock("../../lib/auth/capabilities", () => ({
  hasAdminCapability: (capability: string) =>
    [
      "hrms.employees.read",
      "hrms.leave.request",
      "hrms.leave.approve",
      "hrms.leave.cancel",
    ].includes(capability),
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

vi.mock("./leaveApi", () => ({
  leaveApi: {
    list: mocks.list,
    create: mocks.create,
    approve: mocks.approve,
    reject: mocks.reject,
    cancel: mocks.cancel,
  },
}));

import { LeavePage } from "./LeavePage";

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

const pendingLeave: LeaveResponse = {
  id: "leave-1",
  organizationId: "org-1",
  employeeId: "emp-1",
  leaveType: "ANNUAL",
  startDate: "2026-08-25",
  endDate: "2026-08-26",
  reason: "Family work",
  status: "PENDING",
  requestedBy: "user-1",
  decidedBy: null,
  decidedAt: null,
  decisionComment: null,
  createdAt: "2026-08-20T09:00:00",
  updatedAt: "2026-08-20T09:00:00",
};

const approvedLeave: LeaveResponse = {
  ...pendingLeave,
  status: "APPROVED",
  decidedBy: "manager-1",
  decidedAt: "2026-08-20T10:00:00",
  decisionComment: "Approved",
};

function page(content: LeaveResponse[]) {
  return {
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: content.length > 0 ? 1 : 0,
  };
}

describe("LeavePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.employees.mockResolvedValue({
      content: [employee],
      page: 0,
      size: 100,
      totalElements: 1,
      totalPages: 1,
    });
    mocks.list
      .mockResolvedValueOnce(page([pendingLeave]))
      .mockResolvedValue(page([approvedLeave]));
    mocks.approve.mockResolvedValue(approvedLeave);
    mocks.reject.mockResolvedValue({ ...pendingLeave, status: "REJECTED" });
    mocks.cancel.mockResolvedValue({ ...pendingLeave, status: "CANCELLED" });
  });

  afterEach(cleanup);

  it("shows pending actions only while the request is actionable", async () => {
    render(<LeavePage />);

    const leaveTypeCell = await screen.findByText("ANNUAL");
    fireEvent.click(leaveTypeCell);

    expect(screen.getByRole("button", { name: "Approve" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Reject" })).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Cancel request" }),
    ).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Decision comment"), {
      target: { value: "Approved" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Approve" }));

    await waitFor(() =>
      expect(mocks.approve).toHaveBeenCalledWith("leave-1", {
        organizationId: "org-1",
        decisionComment: "Approved",
      }),
    );

    await waitFor(() =>
      expect(screen.queryByRole("button", { name: "Approve" })).not.toBeInTheDocument(),
    );
    expect(screen.queryByRole("button", { name: "Reject" })).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Cancel request" }),
    ).not.toBeInTheDocument();
    expect(screen.getAllByText("APPROVED").length).toBeGreaterThan(0);
  });
});
