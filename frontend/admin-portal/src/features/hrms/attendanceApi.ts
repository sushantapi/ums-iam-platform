import type { PageResponse } from "../../lib/api";
import {
  hrmsGatewayRequest,
  withHrmsQuery,
} from "./hrmsGatewayClient";

export type AttendanceStatus =
  | "PRESENT"
  | "ABSENT"
  | "HALF_DAY"
  | "HOLIDAY";

export type AttendanceResponse = {
  id: string;
  organizationId: string;
  employeeId: string;
  workDate: string;
  status: AttendanceStatus;
  checkInAt: string | null;
  checkOutAt: string | null;
  notes: string | null;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
};

export type CreateAttendanceRequest = {
  organizationId: string;
  employeeId: string;
  workDate: string;
  status: AttendanceStatus;
  checkInAt?: string | null;
  checkOutAt?: string | null;
  notes?: string | null;
};

export type UpdateAttendanceRequest = {
  organizationId: string;
  status: AttendanceStatus;
  checkInAt?: string | null;
  checkOutAt?: string | null;
  notes?: string | null;
};

export const attendanceApi = {
  list: (query: { organizationId: string; page: number; size: number }) =>
    hrmsGatewayRequest<PageResponse<AttendanceResponse>>(
      withHrmsQuery("/api/v1/hrms/attendance", query),
    ),
  detail: (attendanceId: string, organizationId: string) =>
    hrmsGatewayRequest<AttendanceResponse>(
      withHrmsQuery(`/api/v1/hrms/attendance/${attendanceId}`, {
        organizationId,
      }),
    ),
  create: (body: CreateAttendanceRequest) =>
    hrmsGatewayRequest<AttendanceResponse>("/api/v1/hrms/attendance", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  update: (attendanceId: string, body: UpdateAttendanceRequest) =>
    hrmsGatewayRequest<AttendanceResponse>(
      `/api/v1/hrms/attendance/${attendanceId}`,
      {
        method: "PUT",
        body: JSON.stringify(body),
      },
    ),
};
