import type { PageResponse } from "../../lib/api";
import {
  hrmsGatewayRequest,
  withHrmsQuery,
} from "./hrmsGatewayClient";

export type LeaveType = "ANNUAL" | "SICK" | "CASUAL" | "UNPAID";
export type LeaveStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";

export type LeaveResponse = {
  id: string;
  organizationId: string;
  employeeId: string;
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  reason: string | null;
  status: LeaveStatus;
  requestedBy: string;
  decidedBy: string | null;
  decidedAt: string | null;
  decisionComment: string | null;
  createdAt: string;
  updatedAt: string;
};

export type CreateLeaveRequest = {
  organizationId: string;
  employeeId: string;
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  reason?: string | null;
};

export type LeaveTransitionRequest = {
  organizationId: string;
  decisionComment?: string | null;
};

export const leaveApi = {
  list: (query: { organizationId: string; page: number; size: number }) =>
    hrmsGatewayRequest<PageResponse<LeaveResponse>>(
      withHrmsQuery("/api/v1/hrms/leaves", query),
    ),
  detail: (leaveId: string, organizationId: string) =>
    hrmsGatewayRequest<LeaveResponse>(
      withHrmsQuery(`/api/v1/hrms/leaves/${leaveId}`, { organizationId }),
    ),
  create: (body: CreateLeaveRequest) =>
    hrmsGatewayRequest<LeaveResponse>("/api/v1/hrms/leaves", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  approve: (leaveId: string, body: LeaveTransitionRequest) =>
    hrmsGatewayRequest<LeaveResponse>(`/api/v1/hrms/leaves/${leaveId}/approve`, {
      method: "POST",
      body: JSON.stringify(body),
    }),
  reject: (leaveId: string, body: LeaveTransitionRequest) =>
    hrmsGatewayRequest<LeaveResponse>(`/api/v1/hrms/leaves/${leaveId}/reject`, {
      method: "POST",
      body: JSON.stringify(body),
    }),
  cancel: (leaveId: string, body: LeaveTransitionRequest) =>
    hrmsGatewayRequest<LeaveResponse>(`/api/v1/hrms/leaves/${leaveId}/cancel`, {
      method: "POST",
      body: JSON.stringify(body),
    }),
};
