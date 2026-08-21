import {
  BadgeCheck,
  Building2,
  ClipboardCheck,
  FileClock,
  Landmark,
  Users,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";

import type { AdminCapability } from "../../lib/auth/capabilities";

type HrmsNavigationItem = {
  title: string;
  path: string;
  icon: LucideIcon;
  status: "Live starter";
  requiredCapability: AdminCapability;
};

type HrmsNavigationSection = {
  section: "HRMS";
  screens: HrmsNavigationItem[];
};

export const hrmsSidebarSection: HrmsNavigationSection = {
  section: "HRMS",
  screens: [
    {
      title: "Employees",
      path: "/hrms/employees",
      icon: Users,
      status: "Live starter",
      requiredCapability: "hrms.employees.read",
    },
    {
      title: "Departments",
      path: "/hrms/departments",
      icon: Building2,
      status: "Live starter",
      requiredCapability: "hrms.departments.read",
    },
    {
      title: "Designations",
      path: "/hrms/designations",
      icon: BadgeCheck,
      status: "Live starter",
      requiredCapability: "hrms.designations.read",
    },
    {
      title: "Attendance",
      path: "/hrms/attendance",
      icon: ClipboardCheck,
      status: "Live starter",
      requiredCapability: "hrms.attendance.read",
    },
    {
      title: "Leave",
      path: "/hrms/leave",
      icon: FileClock,
      status: "Live starter",
      requiredCapability: "hrms.leave.read",
    },
    {
      title: "Payroll",
      path: "/hrms/payroll",
      icon: Landmark,
      status: "Live starter",
      requiredCapability: "hrms.payroll.read",
    },
  ],
};
