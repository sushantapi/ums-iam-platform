import { Route } from "react-router-dom";

import { RequireCapability } from "../../lib/auth/RequireCapability";
import { AttendancePage } from "./AttendancePage";
import { EmployeeDetailPage } from "./EmployeeDetailPage";
import { EmployeesPage } from "./EmployeesPage";
import { HrmsMasterDataPage } from "./HrmsMasterDataPage";
import { LeavePage } from "./LeavePage";
import { PayrollPage } from "./PayrollPage";

export const hrmsRoutes = (
  <>
    <Route
      path="/hrms/employees"
      element={
        <RequireCapability capability="hrms.employees.read">
          <EmployeesPage />
        </RequireCapability>
      }
    />
    <Route
      path="/hrms/employees/:employeeId"
      element={
        <RequireCapability capability="hrms.employees.read">
          <EmployeeDetailPage />
        </RequireCapability>
      }
    />
    <Route
      path="/hrms/departments"
      element={
        <RequireCapability capability="hrms.departments.read">
          <HrmsMasterDataPage kind="departments" />
        </RequireCapability>
      }
    />
    <Route
      path="/hrms/designations"
      element={
        <RequireCapability capability="hrms.designations.read">
          <HrmsMasterDataPage kind="designations" />
        </RequireCapability>
      }
    />
    <Route
      path="/hrms/attendance"
      element={
        <RequireCapability capability="hrms.attendance.read">
          <AttendancePage />
        </RequireCapability>
      }
    />
    <Route
      path="/hrms/leave"
      element={
        <RequireCapability capability="hrms.leave.read">
          <LeavePage />
        </RequireCapability>
      }
    />
    <Route
      path="/hrms/payroll"
      element={
        <RequireCapability capability="hrms.payroll.read">
          <PayrollPage />
        </RequireCapability>
      }
    />
  </>
);
