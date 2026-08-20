import { Route } from "react-router-dom";

import { RequireCapability } from "../../lib/auth/RequireCapability";
import { HrmsModulePage } from "./HrmsModulePage";

export const hrmsRoutes = (
  <>
    <Route
      path="/hrms/employees"
      element={
        <RequireCapability capability="hrms.employees.read">
          <HrmsModulePage module="employees" />
        </RequireCapability>
      }
    />
    <Route
      path="/hrms/departments"
      element={
        <RequireCapability capability="hrms.departments.read">
          <HrmsModulePage module="departments" />
        </RequireCapability>
      }
    />
    <Route
      path="/hrms/designations"
      element={
        <RequireCapability capability="hrms.designations.read">
          <HrmsModulePage module="designations" />
        </RequireCapability>
      }
    />
    <Route
      path="/hrms/attendance"
      element={
        <RequireCapability capability="hrms.attendance.read">
          <HrmsModulePage module="attendance" />
        </RequireCapability>
      }
    />
    <Route
      path="/hrms/leave"
      element={
        <RequireCapability capability="hrms.leave.read">
          <HrmsModulePage module="leave" />
        </RequireCapability>
      }
    />
    <Route
      path="/hrms/payroll"
      element={
        <RequireCapability capability="hrms.payroll.read">
          <HrmsModulePage module="payroll" />
        </RequireCapability>
      }
    />
  </>
);
